package com.dtolabs.rundeck.core.execution.workflow.state;

import com.dtolabs.rundeck.core.utils.PairImpl;

import java.util.*;

/**
 * $INTERFACE is ... User: greg Date: 10/17/13 Time: 12:36 PM
 */
public class StateUtils {

    public static StepState stepState(ExecutionState state) {
        return stepState(state, null, null, null, null, null);
    }

    public static StepState stepState(ExecutionState state, Map metadata) {
        return stepState(state, metadata, null, null, null, null);
    }

    public static StepState stepState(ExecutionState state, Map metadata, String errorMessage) {
        return stepState(state, metadata, errorMessage, null, null, null);
    }

    public static StepState stepState(ExecutionState state, Map metadata, String errorMessage,
            Date startTime,
            Date updateTime, Date endTime) {

        StepStateImpl stepState = new StepStateImpl();
        stepState.setExecutionState(state);
        stepState.setMetadata(metadata);
        stepState.setErrorMessage(errorMessage);
        stepState.setStartTime(startTime);
        stepState.setUpdateTime(updateTime);
        stepState.setEndTime(endTime);
        return stepState;
    }

    public static StepStateChange stepStateChange(StepState state) {
        StepStateChangeImpl stepStateChange = new StepStateChangeImpl();
        stepStateChange.setStepState(state);
        stepStateChange.setNodeState(false);
        return stepStateChange;
    }

    public static StepStateChange stepStateChange(StepState state, String nodeName) {
        StepStateChangeImpl stepStateChange = new StepStateChangeImpl();
        stepStateChange.setStepState(state);
        stepStateChange.setNodeState(null != nodeName);
        stepStateChange.setNodeName(nodeName);
        return stepStateChange;
    }

    public static class CtxItem extends PairImpl<Integer, Boolean> implements StepContextId {
        public CtxItem(Integer first, Boolean second) {
            super(first, second);
        }

        public int getStep() {
            return getFirst();
        }

        @Override
        public StepAspect getAspect() {
            return getSecond() ? StepAspect.ErrorHandler : StepAspect.Main;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CtxItem that = (CtxItem) o;

            if (getStep() != that.getStep()) return false;
            if (!getAspect().equals(that.getAspect())) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + getStep();
            result = 31 * result + getAspect().hashCode();
            return result;
        }

        @Override
        public int compareTo(StepContextId o) {
            int step = this.getStep();
            int thatstep = o.getStep();
            int c = step < thatstep ? -1 : step > thatstep ? 1 : 0;
            if (c != 0) {
                return c;
            }
            return this.getAspect().compareTo(o.getAspect());
        }
        public String toString() {
            return getStep() + (getSecond() ? "e" : "");
        }
    }

    public static StepContextId stepContextId(int step, boolean errorhandler) {
        return new CtxItem(step, errorhandler);
    }

    public static StepIdentifier stepIdentifier(List<StepContextId> context) {
        return new StepIdentifierImpl(context);
    }

    public static StepIdentifier stepIdentifier(StepContextId... context) {
        return new StepIdentifierImpl(Arrays.asList(context));
    }

    public static StepIdentifier stepIdentifier(int id) {
        return stepIdentifier(stepContextId(id, false));
    }

    public static StepIdentifier stepIdentifier(int... ids) {
        return stepIdentifier(asStepContextIds(ids));
    }

    public static StepContextId last(StepIdentifier stepIdentifier) {
        return stepIdentifier.getContext().get(stepIdentifier.getContext().size() - 1);
    }

    public static StepContextId first(StepIdentifier stepIdentifier) {
        return stepIdentifier.getContext().get(0);
    }

    private static List<StepContextId> asStepContextIds(int[] ids) {
        ArrayList<StepContextId> stepContextIds = new ArrayList<StepContextId>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            stepContextIds.add(stepContextId(id, false));
        }
        return stepContextIds;
    }


    public static StepIdentifier stepIdentifierTail(StepIdentifier identifier) {
        return new StepIdentifierImpl(identifier.getContext().subList(1, identifier.getContext().size()));
    }

    public static StepIdentifier stepIdentifierAppend(StepIdentifier identifier, StepIdentifier identifier2) {
        ArrayList<StepContextId> context = new ArrayList<StepContextId>();
        if (null != identifier && null != identifier.getContext() && identifier.getContext().size() > 0) {
            context.addAll(identifier.getContext());
        }
        if (null != identifier2 && null != identifier2.getContext() && identifier2.getContext().size() > 0) {
            context.addAll(identifier2.getContext());
        }
        return new StepIdentifierImpl(context);
    }


    public static WorkflowState workflowState(List<String> nodeSet, List<String> allNodes, long stepCount,
            ExecutionState executionState,
            Date updateTime,
            Date startTime,
            Date endTime,
            String serverNode,
            List<WorkflowStepState> stepStates,
            boolean setupNodeStates
    ) {
        WorkflowStateImpl workflowState = new WorkflowStateImpl(nodeSet, allNodes, stepCount, executionState,
                updateTime, startTime, endTime, serverNode,stepStates, null);
        if (setupNodeStates) {
            setupNodeStates(workflowState);
        }
        return workflowState;
    }

    public static WorkflowState workflowState(List<String> nodeSet, List<String> allNodes, long stepCount,
            ExecutionState executionState,
            Date timestamp,
            Date startTime,
            Date endTime,
            String serverNode,
            List<WorkflowStepState> stepStates,
            Map<String, WorkflowNodeState> nodeStates
    ) {
        return new WorkflowStateImpl(nodeSet, allNodes, stepCount, executionState, timestamp, startTime, endTime, serverNode,
                stepStates, nodeStates);
    }

    /**
     * Configure the nodeStates map for the workflow, by visiting each step in the workflow, and connecting the
     * step+node state for nodeSteps to the nodeStates map
     *
     * @param current workflow to visit
     */
    public static void setupNodeStates(WorkflowStateImpl current) {
        setupNodeStates(current, current, null);
    }

    /**
     * Configure the nodeStates map for the workflow, by visiting each step in the workflow, and connecting the
     * step+node state for nodeSteps to the nodeStates map
     *
     * @param current workflow to visit
     * @param parent  root workflow impl
     * @param ident   parent workflow ident or null
     */
    private static void setupNodeStates(WorkflowState current, WorkflowStateImpl parent, StepIdentifier ident) {
        //create a new mutable map
        HashMap<String, WorkflowNodeState> nodeStates = new HashMap<String, WorkflowNodeState>();
        if (null != parent.getNodeStates()) {
            //include original states
            nodeStates.putAll(parent.getNodeStates());
        }
        ArrayList<String> allNodes = new ArrayList<String>();
        if(null!=parent.getNodeSet()) {
            allNodes.addAll(parent.getNodeSet());
        }
        parent.setNodeStates(nodeStates);
        parent.setAllNodes(allNodes);

        for (WorkflowStepState workflowStepState : current.getStepStates()) {
            StepIdentifier thisident = stepIdentifierAppend(ident, workflowStepState.getStepIdentifier());
            if (workflowStepState.isNodeStep()) {
                //add node states for this step
                for (String nodeName : current.getNodeSet()) {
                    //new mutable map to store step states for this node
                    HashMap<StepIdentifier, StepState> stepStatesForNode = new HashMap<StepIdentifier, StepState>();

                    //get node state for this step
                    StepState state = workflowStepState.getNodeStateMap().get(nodeName);
                    stepStatesForNode.put(thisident, state);

                    //if already exists an entry for this node in the workflow's node states, import the states
                    WorkflowNodeState orig = nodeStates.get(nodeName);
                    if (null != orig && null != orig.getStepStateMap()) {
                        stepStatesForNode.putAll(orig.getStepStateMap());
                    }
                    if (!allNodes.contains(nodeName)) {
                        allNodes.add(nodeName);
                    }

                    //create new workflowNodeState for this node
                    WorkflowNodeState workflowNodeState = workflowNodeState(nodeName, state, thisident,
                            stepStatesForNode);
                    nodeStates.put(nodeName, workflowNodeState);
                }
            } else if (workflowStepState.hasSubWorkflow()) {
                //descend
                setupNodeStates(workflowStepState.getSubWorkflowState(), parent, thisident);
            }
        }
    }

    public static WorkflowStepState workflowStepState(StepState stepState, Map<String, StepState> nodeStateMap,
            StepIdentifier stepIdentifier, WorkflowState subWorkflowState, List<String> nodeStepTargets,
            boolean nodeStep) {
        WorkflowStepStateImpl workflowStepState = new WorkflowStepStateImpl();
        workflowStepState.setStepState(stepState);
        workflowStepState.setNodeStateMap(nodeStateMap);
        workflowStepState.setStepIdentifier(stepIdentifier);
        workflowStepState.setSubWorkflow(null != subWorkflowState);
        workflowStepState.setSubWorkflowState(subWorkflowState);
        if (null != nodeStepTargets) {
            workflowStepState.setNodeStepTargets(nodeStepTargets);
        }
        workflowStepState.setNodeStep(nodeStep);
        return workflowStepState;
    }

    public static WorkflowNodeState workflowNodeState(String nodeName, StepState nodeState,
            StepIdentifier lastIdentifier,
            Map<StepIdentifier, ? extends StepState> stepStates) {
        return workflowNodeStateImpl(nodeName, nodeState, lastIdentifier, stepStates);
    }

    private static WorkflowNodeStateImpl workflowNodeStateImpl(String nodeName, StepState nodeState, StepIdentifier
            lastIdentifier, Map<StepIdentifier, ? extends StepState> stepStates) {
        WorkflowNodeStateImpl workflowNodeState = new WorkflowNodeStateImpl();
        workflowNodeState.setNodeName(nodeName);
        workflowNodeState.setNodeState(nodeState);
        workflowNodeState.setLastIdentifier(lastIdentifier);
        workflowNodeState.setStepStateMap(stepStates);
        return workflowNodeState;
    }
}
