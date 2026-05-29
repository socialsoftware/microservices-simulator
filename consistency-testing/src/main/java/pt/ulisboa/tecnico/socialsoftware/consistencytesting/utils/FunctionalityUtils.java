package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public class FunctionalityUtils {

    private FunctionalityUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns an immutable list of the workflow steps for the given functionality,
     * in order according to the execution plan.
     *
     * @param func the workflow functionality
     * @return an immutable list of flow steps
     */
    public static List<FlowStep> getSteps(WorkflowFunctionality func) {
        Workflow workflow = func.getWorkflow();
        ExecutionPlan plan = workflow.getOrCreateExecutionPlan();
        ArrayList<FlowStep> funcSteps = Objects.requireNonNull(plan.getPlan());
        return List.copyOf(funcSteps);
    }
}
