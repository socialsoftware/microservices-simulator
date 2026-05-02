package pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils;

import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;

public class WorkflowUtils {

    public static List<FlowStep> getWorkflowSteps(Workflow workflow) {
        ExecutionPlan plan = workflow.getOrCreateExecutionPlan();
        Objects.requireNonNull(plan);
        return Objects.requireNonNull(plan.getPlan());
    }
}
