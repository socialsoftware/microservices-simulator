package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;

public class ExecutionPlan {
    private ArrayList<Runnable> plan;
    public void execute(){
        for (Runnable step: plan) {
            step.run();
        }
    }
}
