/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.workflowsim.examples.failure;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.workflowsim.CondorVM;
import org.workflowsim.DatacenterExtended;
import org.workflowsim.Job;
import org.workflowsim.WorkflowEngine;
import org.workflowsim.WorkflowPlanner;
import org.workflowsim.examples.WorkflowSimBasicExample1;
import org.workflowsim.failure.FailureGenerator;
import org.workflowsim.failure.FailureMonitor;
import org.workflowsim.failure.FailureParameters;
import org.workflowsim.utils.ClusteringParameters;
import org.workflowsim.utils.OverheadParameters;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.ReplicaCatalog;

/**
 * This FaultTolerantExample1 uses FailureGenerator to create task failures and 
 * then retry tasks
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Dec 31, 2013
 */
public class FaultTolerantSchedulingExample1 extends WorkflowSimBasicExample1 {

    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example
     * This example has only one datacenter and one storage
     */
    public static void main(String[] args) {


       try {
            // First step: Initialize the WorkflowSim package. 

            /**
             * However, the exact number of vms may not necessarily be vmNum If
             * the data center or the host doesn't have sufficient resources the
             * exact vmNum would be smaller than that. Take care.
             */
            int vmNum = 20;//number of vms;
            /**
             * Should change this based on real physical path
             */
            String daxPath = "/Users/chenweiwei/Work/WorkflowSim-1.0/config/dax/Montage_100.xml";
            if(daxPath == null){
                Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");
                return;
            }
            File daxFile = new File(daxPath);
            if(!daxFile.exists()){
                Log.printLine("Warning: Please replace daxPath with the physical path in your working environment!");
                return;
            }
            /*
             *  Fault Tolerant Parameters
             */
            /**
             * MONITOR_JOB classifies failures based on the level of jobs; MONITOR_VM classifies failures
             * based on the vm id; MOINTOR_ALL does not do any classification; MONITOR_NONE does not record
             * any failiure. 
             */
            FailureParameters.FTCMonitor ftc_monitor = FailureParameters.FTCMonitor.MONITOR_ALL;
            /**
             *  Similar to FTCMonitor, FTCFailure controls the way how we generate failures. 
             */
            FailureParameters.FTCFailure ftc_failure = FailureParameters.FTCFailure.FAILURE_ALL;
            /**
             *  In this example, we have no clustering and thus it is no need to do 
             * Fault Tolerant Clustering. By default, WorkflowSim will just rety all
             * the failed task. 
             */
            FailureParameters.FTCluteringAlgorithm ftc_method = FailureParameters.FTCluteringAlgorithm.FTCLUSTERING_NOOP;
            /**
             * Task failure rate for each level 
             * 
             */
            Map<Integer, Double> taskFailureMap = new HashMap();
            int maxLevel = 11; //most workflows we use has a maximum of 11 levels
            for(int level = 0; level < maxLevel; level ++ ){
                /*
                 * For simplicity, set the task failure rate of each level to be 0.1. Which means 10%
                 * of submitted tasks will fail. It doesn't have to be the same task 
                 * failure rate at each level. 
                 */
                taskFailureMap.put(level, 0.1);
            }
            
            

            /**
             * Since we are using MINMIN scheduling algorithm, the planning algorithm should be INVALID 
             * such that the planner would not override the result of the scheduler
             */
            Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.MINMIN;
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.INVALID;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.SHARED;

            /**
             * No overheads 
             */
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);;
            
            /**
             * No Clustering
             */
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);

            /**
             * Initialize static parameters
             */
            FailureParameters.init(ftc_method, ftc_monitor, ftc_failure, taskFailureMap);
            Parameters.init(vmNum, daxPath, null,
                    null, op, cp, sch_method, pln_method,
                    null, 0);
            ReplicaCatalog.init(file_system);

            FailureMonitor.init();
            FailureGenerator.init();

            // before creating any entities.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            DatacenterExtended datacenter0 = createDatacenter("Datacenter_0");

            /**
             * Create a WorkflowPlanner with one schedulers.
             */
            WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);
            /**
             * Create a WorkflowEngine.
             */
            WorkflowEngine wfEngine = wfPlanner.getWorkflowEngine();
            /**
             * Create a list of VMs.The userId of a vm is basically the id of the scheduler
             * that controls this vm. 
             */
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), Parameters.getVmNum());

            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmlist0, 0);

            /**
             * Binds the data centers with the scheduler.
             */
            wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);

            CloudSim.startSimulation();


            List<Job> outputList0 = wfEngine.getJobsReceivedList();

            CloudSim.stopSimulation();

            printJobList(outputList0);
            

        } catch (Exception e) {
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

}
