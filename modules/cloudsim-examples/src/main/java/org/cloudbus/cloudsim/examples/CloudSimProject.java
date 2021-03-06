/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 * Last modified: 20/10/17
 * Note:		Code modified to demonstrate priority based sorting and testing to cloudlets
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
import java.util.concurrent.TimeUnit;

// This class is used to define a new object that houses the priority level and cloudlet id properties, and refresh
// ... method in order to calculate a priority value for each task that can be dynamically updated
class Task {
	int cloudletIndex;
	int priorityLevel;			// Assigned a value from 1 to 10 (not enforced yet)
	float priority;				// The priority value calculated using priorityLevel and the taskTime
	int taskStartTime;			// When the task started (in ms)
	int taskTime;				// How long the task has been waiting

	// Initialises a task object, sets passed values to object properties
	Task(int index, int p_level){
		this.cloudletIndex = index;			// sets the cloudlet index
		this.priorityLevel = p_level;		// sets the priority level

		this.taskStartTime = (int) System.currentTimeMillis();	// sets the current time (initial task time)
		this.priority = this.priorityLevel * 100;				// doesn't use taskTime as task has just been initialised
	}

	// Finds the time the task has been waiting and recalculates its priority value
	public void refreshTask() {
		// retrieves time waiting
		this.taskTime = (int) (System.currentTimeMillis() - this.taskStartTime);
		// Equation below defines how priority escalates with time
		this.priority = this.priorityLevel * 100 + (this.taskTime*this.priorityLevel/1000);
	}

	// Used to nicely format output header
	public void printHeader() {
		System.out.printf("%-17s%-17s%-17s%-17s\n",
				"Cloudlet Index", "Priority Level", "Priority Value", "Time Elapsed");
	}

	// Nicely formats task output
	public void printTask() {
		System.out.printf("%-17d%-17d%-17.2f% -17d\n",
				this.cloudletIndex, this.priorityLevel, this.priority, this.taskTime);
	}
}


/**
 * A class defining the requirements of a cloudSim project
 */
public class CloudSimProject {

	/** Initialises the cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** Initialises the VMlist. */
	private static List<Vm> vmlist;

	/**
	 * Creates main() to run the project
	 */
	public static void main(String[] args) {

		Log.printLine("Starting CloudSimProject...");

	        try {
	        	// First step: Initialize the CloudSim package. It should be called
				// before creating any entities.
				int num_user = 1;   							// number of cloud users
				Calendar calendar = Calendar.getInstance();
				boolean trace_flag = false;  					// mean trace events

				// Initialize the CloudSim library
				CloudSim.init(num_user, calendar, trace_flag);

				// Second step: Create Datacenters
				// Datacenters are the resource providers in CloudSim (at least one needed to run a CloudSim simulation)
				@SuppressWarnings("unused")
				Datacenter datacenter0 = createDatacenter("Datacenter_0");

				// Third step: Create Broker
				DatacenterBroker broker = createBroker();
				int brokerId = broker.getId();

				// Fourth step: Create some virtual machines
				vmlist = new ArrayList<Vm>();

				// VM description
				int vmid;			// ID of the VM
				int mips = 250;			// (Millions of Instructions Per Second)
				long size = 10000; 		// image size (MB) - given amount of storage
				int ram = 512; 			// vm memory (MB) - RAM
				long bw = 1000;			// Bandwidth
 				int pesNumber = 1; 		// number of CPUs
				String vmm = "Xen"; 	// VM name

				// Create some VMs for testing
				for (vmid = 0; vmid < 4; vmid++){
					Vm vm1 = new Vm(vmid, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
					vmlist.add(vm1);	// Add the VM to the list
				}
				// Can use CloudletSchedulerSpaceShared instead of CloudletSchedulerTimeShared to run cloudlets
				// ... consecutively instead of concurrently

				// Submit vm list to the broker
				broker.submitVmList(vmlist);

				// Fifth step: Create some Cloudlets
				cloudletList = new ArrayList<Cloudlet>();
				List<Task> priorityList = new ArrayList<Task>();		// **** used to store the cloudlet priority information
				List<Cloudlet> submissionList = new ArrayList<Cloudlet>();	// **** used to store the cloudlet priority information

				// Test Cloudlet properties
				int id;
				pesNumber = 1;				// Number of processing elements required to execute the cloudlet
				long length = 250000;		// Execution length of cloudlet (in MIPS)
				long fileSize = 300;		// Size of the cloudlet on input (the program + input data sizes)
				long outputSize = 300;		// Output size of the cloudlet (in Bytes)
				UtilizationModel utilizationModel = new UtilizationModelFull();
				int [] priorityTestVals = {2, 1, 5, 6, 3, 4, 2, 5};

				// Creates a number of cloudlets with different properties for testing purposes
				for (id = 0; id < 8; id++) {
					// Define cloudlets, task object, and add them to their respective lists
					Cloudlet cloudlet1 = new Cloudlet(id, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
					cloudletList.add(cloudlet1);
					priorityList.add(new Task(id, priorityTestVals[id]));
					cloudlet1.setUserId(brokerId);
				}

				for (int i = 0; i < id; i++){
					TimeUnit.SECONDS.sleep(2);				// Puts the program to sleep for 12 seconds
					priorityList.get(i).refreshTask();		// Refreshes the priority calculation for only one task
				}

				// Prints the List of unsorted tasks
				System.out.println("-*-*-*-*-*-*-*-*-*-*-*- Unsorted Cloudlets -*-*-*-*-*-*-*-*-*-*-*-");
				System.out.printf("%-15s\t%-15s\t%-15s\t%-15s\n",
						"Cloudlet Index", " Priority Level", "  Priority Value", "Time Elapsed");
				priorityList.stream().forEach((t) -> {
					t.printTask();
				});

				// Sorts the list of tasks objects by their priority (in descending order) using a lambda function
				Collections.sort(priorityList, ( Task t1, Task t2 ) -> Float.compare(t2.priority, t1.priority));
				// This method of sorting can also be implemented on the Cloudlet list it's decided to
				// ... add the priority attribute to the Cloudlet class

				// Prints the List of sorted tasks
				System.out.println("-*-*-*-*-*-*-*-*-*-*-*-  Sorted Cloudlets  -*-*-*-*-*-*-*-*-*-*-*-");
				System.out.printf("%-15s\t%-15s\t%-15s\t%-15s\n",
						"Cloudlet Index", " Priority Level", "  Priority Value", "Time Elapsed");
				priorityList.stream().forEach((t) -> {
					t.printTask();
				});

				// Adds each cloudlet to the submissionList in priority order
				priorityList.forEach((t) -> submissionList.add(cloudletList.get(t.cloudletIndex)));

				// Submit the (old) cloudlet list to the broker
				//broker.submitCloudletList(cloudletList);
				// OR
				// Submit new cloudlet list to the broker
				broker.submitCloudletList(submissionList);

				// Sixth step: Starts the simulation
				CloudSim.startSimulation();

				// Final step: Print results when simulation is over
				List<Cloudlet> newList = broker.getCloudletReceivedList();

				CloudSim.stopSimulation();
				printCloudletList(newList);
				Log.printLine("CloudSimProject finished!");
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	            Log.printLine("The simulation has been terminated due to an unexpected error");
	        }
		// Example of how to bind cloudlets to a VM
		//broker.bindCloudletToVm(cloudlet1.getCloudletId(),vm1.getId());
		// How to clear the cloudlet list
		//cloudletList.clear();
		// Retrieves some test metrics
//				for(Cloudlet cl: newList){
//					System.out.println(cl.getActualCPUTime());
//				}
	    }

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
			String h_indent = "  ";
	        Log.printLine();
	        Log.printLine("========== OUTPUT ==========");
	        Log.printLine("Cloudlet ID" + h_indent + "STATUS" + h_indent +
	                "  Data center ID" + h_indent + " VM ID" + h_indent + "   Time" + h_indent + "  Start Time" + h_indent + "  Finish Time");

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
