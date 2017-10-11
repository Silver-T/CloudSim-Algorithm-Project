/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.examples;


import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;

class Task {
	int cloudlet_index;
	float priority_level;
	float priority;
	float start_time;

	public Task(int index, float p_level, float time){
		this.cloudlet_index = index;
		this.priority_level = p_level;

		this.start_time = time - System.currentTimeMillis();
		this.priority = this.priority_level * 1000 + this.start_time;
	}

	public void printHeader() {
		System.out.printf("%-17s%-17s%-17s%-17s\n",
				"Cloudlet Index", "Priority Level", "Priority Value", "Time Elapsed");
	}

	public void printTask() {
		System.out.printf("%-17d%-17f%-17f%-17f\n",
				this.cloudlet_index, this.priority_level, this.priority, this.start_time);
	}
}


/**
 * A simple example showing how to create
 * a datacenter with one host and run two
 * cloudlets on it. The cloudlets run in
 * VMs with the same MIPS requirements.
 * The cloudlets will take the same time to
 * complete the execution.
 */
public class CloudSimProject {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {

		Log.printLine("Starting CloudSimExample2...");

	        try {
	        	// First step: Initialize the CloudSim package. It should be called
				// before creating any entities.
				int num_user = 1;   // number of cloud users
				Calendar calendar = Calendar.getInstance();
				boolean trace_flag = false;  // mean trace events
				float start_time = System.currentTimeMillis();

				// Initialize the CloudSim library
				CloudSim.init(num_user, calendar, trace_flag);

				// Second step: Create Datacenters
				//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
				@SuppressWarnings("unused")
				Datacenter datacenter0 = createDatacenter("Datacenter_0");

				//Third step: Create Broker
				DatacenterBroker broker = createBroker();
				int brokerId = broker.getId();

				//Fourth step: Create one virtual machine
				vmlist = new ArrayList<Vm>();

				//VM description
				int vmid = 0;
				int mips = 250;
				long size = 10000; //image size (MB)
				int ram = 512; //vm memory (MB)
				long bw = 1000;
				int pesNumber = 1; //number of cpus
				String vmm = "Xen"; //VMM name

				//create two VMs
				Vm vm1 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
				// **** can use CloudletSchedulerSpaceShared to run cloudlets consecutively

				vmid++;
				Vm vm2 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());

				//add the VMs to the vmList
				vmlist.add(vm1);
				vmlist.add(vm2);

				//submit vm list to the broker
				broker.submitVmList(vmlist);

				//Fifth step: Create two Cloudlets
				cloudletList = new ArrayList<Cloudlet>();			// **** made this a linked list
				List<Task> priorityList = new ArrayList<Task>();								// **** used to store the cloudlet priorities

				//Cloudlet properties
				int id = 0;
				pesNumber = 1;	// Number of processing elements required to execute the cloudlet
				long length = 250000;
				long fileSize = 300;
				long outputSize = 300;
				UtilizationModel utilizationModel = new UtilizationModelFull();

				Cloudlet cloudlet1 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
				priorityList.add(new Task(id, 2.0f, start_time));
				cloudlet1.setUserId(brokerId);

				id++;
				Cloudlet cloudlet2 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
				priorityList.add(new Task(id, 1.0f, start_time));
				cloudlet2.setUserId(brokerId);

				// ****
				id++;
				Cloudlet cloudlet3 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
				priorityList.add(new Task(id, 5.0f, start_time));
				cloudlet3.setUserId(brokerId);

				// ****
				id++;
				Cloudlet cloudlet4 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
				priorityList.add(new Task(id, 1.0f, start_time));
				cloudlet4.setUserId(brokerId);

				//add the cloudlets to the cloudlet list and priorities to the priority list
				cloudletList.add(cloudlet1);
				cloudletList.add(cloudlet2);
				cloudletList.add(cloudlet3);
				cloudletList.add(cloudlet4);

				// **** sort the cloudlet based off priorities list
				System.out.println("-*-*-*-*-*-*- Unsorted");
				System.out.printf("%-15s\t%-15s\t%-15s\t%-15s\n",
						"Cloudlet Index", "Priority Level", "Priority Value", "Time Elapsed");
				priorityList.stream().forEach((t) -> {
					t.printTask();
				});

				// Sorts the list of tasks objects by their priority (in descending order) using a lambda function
				Collections.sort(priorityList, ( Task t1, Task t2 ) -> Float.compare(t2.priority, t1.priority));

				System.out.println("-*-*-*-*-*-*- Sorted");
				System.out.printf("%-15s\t%-15s\t%-15s\t%-15s\n",
						"Cloudlet Index", "Priority Level", "Priority Value", "Time Elapsed");
				priorityList.stream().forEach((t) -> {
					t.printTask();
				});

				// ****

				//submit cloudlet list to the broker
				broker.submitCloudletList(cloudletList);

				// **** cloudletList.clear();			// **** Clears the list of Cloudlets

				// **** Inserts new cloudlets into the list
				// ****cloudletList.add(cloudlet3);
				// ****cloudletList.add(cloudlet4);

				// **** submit new cloudlet list to the broker
				// ****broker.submitCloudletList(cloudletList);
				// **** need to change submitCloudletList in DatacenterBroker.java to accommodate priority list

				//bind the cloudlets to the vms. This way, the broker
				// will submit the bound cloudlets only to the specific VM
				broker.bindCloudletToVm(cloudlet1.getCloudletId(),vm1.getId());
				broker.bindCloudletToVm(cloudlet2.getCloudletId(),vm2.getId());

				// Sixth step: Starts the simulation
				CloudSim.startSimulation();


				// Final step: Print results when simulation is over
				List<Cloudlet> newList = broker.getCloudletReceivedList();

				// ** Grabs some test metrics ** ??
				for(Cloudlet cl: newList){
					System.out.println(cl.getActualCPUTime());
					//System.out.println(findVmById(cl.getVmId(), broker.getVmList()).getBw());
				}

				CloudSim.stopSimulation();

				printCloudletList(newList);

				Log.printLine("CloudSimExample2 finished!");
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	            Log.printLine("The simulation has been terminated due to an unexpected error");
	        }
	    }
//		////////////////////////////////////////////////
//	    static Vm findVmById(int id,, List<Vm> list){
//			return null;
//		}

		private static Datacenter createDatacenter(String name){

	        // Here are the steps needed to create a PowerDatacenter:
	        // 1. We need to create a list to store
	    	//    our machine
	    	List<Host> hostList = new ArrayList<Host>();

	        // 2. A Machine contains one or more PEs or CPUs/Cores.
	    	// In this example, it will have only one core.
	    	List<Pe> peList = new ArrayList<Pe>();

	    	int mips = 1000;

	        // 3. Create PEs and add these into a list.
	    	peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

	        //4. Create Host with its id and list of PEs and add them to the list of machines
	        int hostId=0;
	        int ram = 2048; //host memory (MB)
	        long storage = 1000000; //host storage
	        int bw = 10000;

	        hostList.add(
	    			new Host(
	    				hostId,
	    				new RamProvisionerSimple(ram),
	    				new BwProvisionerSimple(bw),
	    				storage,
	    				peList,
	    				new VmSchedulerTimeShared(peList)
	    			)
	    		); // This is our machine


	        // 5. Create a DatacenterCharacteristics object that stores the
	        //    properties of a data center: architecture, OS, list of
	        //    Machines, allocation policy: time- or space-shared, time zone
	        //    and its price (G$/Pe time unit).
	        String arch = "x86";      // system architecture
	        String os = "Linux";          // operating system
	        String vmm = "Xen";
	        double time_zone = 10.0;         // time zone this resource located
	        double cost = 3.0;              // the cost of using processing in this resource
	        double costPerMem = 0.05;		// the cost of using memory in this resource
	        double costPerStorage = 0.001;	// the cost of using storage in this resource
	        double costPerBw = 0.0;			// the cost of using bw in this resource
	        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

	        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
	                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


	        // 6. Finally, we need to create a PowerDatacenter object.
	        Datacenter datacenter = null;
	        try {
	            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }

	        return datacenter;
	    }

	    //We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
	    //to the specific rules of the simulated scenario
	    private static DatacenterBroker createBroker(){

	    	DatacenterBroker broker = null;
	        try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	    	return broker;
	    }

	    /**
	     * Prints the Cloudlet objects
	     * @param list  list of Cloudlets
	     */
	    private static void printCloudletList(List<Cloudlet> list) {
	        int size = list.size();
	        Cloudlet cloudlet;

	        String indent = "    ";
	        Log.printLine();
	        Log.printLine("========== OUTPUT ==========");
	        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
	                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

	        DecimalFormat dft = new DecimalFormat("###.##");
	        for (int i = 0; i < size; i++) {
	            cloudlet = list.get(i);
	            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

	            if (cloudlet.getCloudletStatusString() == "Success"){
	                Log.print(cloudlet.getCloudletStatusString());	// Changed from "SUCCESS"

	            	Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
	                     indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
                             indent + indent + dft.format(cloudlet.getFinishTime()));
	            } else {	// **** Added this clause to print status other than success
					Log.printLine(cloudlet.getCloudletStatusString());
				}
	        }

	    }
}
