package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Workflow {
    private UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowData data;
    private HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>();

    public Workflow(WorkflowData data, UnitOfWorkService unitOfWorkService, String functionalityName) {
        this.data = data;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    }

    public Workflow(UnitOfWorkService unitOfWorkService, String functionalityName) {
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    }

    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public void addStep(FlowStep step){
        this.stepsWithDependencies.put(step, step.getDependencies());
    }

    public abstract ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies);

    public void execute() {
        ExecutionPlan executionPlan = planOrder(this.stepsWithDependencies); // redefined for each transaction model

        try {
            executionPlan.execute(); // independent of transactional model
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception e) {
            unitOfWorkService.abort(unitOfWork);
            throw e;
        }
    }
}
