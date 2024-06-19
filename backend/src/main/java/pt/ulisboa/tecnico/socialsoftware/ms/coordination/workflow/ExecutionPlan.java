package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;

public class ExecutionPlan {
    private ArrayList<FlowStep> plan;

    public ExecutionPlan(ArrayList<FlowStep> plan) {
        this.plan = plan;
    }

    public void execute(){
        for (FlowStep step: plan) {
            step.execute();
        }
    }
}
