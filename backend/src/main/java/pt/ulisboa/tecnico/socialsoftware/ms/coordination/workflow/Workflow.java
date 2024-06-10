package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;

import java.util.List;
import java.util.Map;

public abstract class Workflow {
    private UnitOfWorkService unitOfWorkService;
    private UnitOfWork unitOfWork;
    private WorkflowData data;
    private Map<FlowStep, List<FlowStep>> stepsWithDependencies;

    public Workflow(WorkflowData data, UnitOfWorkService unitOfWorkService, String functionalityName) {
        this.data = data;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    }

    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public void addStep(FlowStep step){}

    public abstract ExecutionPlan planOrder();

    public void execute() {
        ExecutionPlan executionPlan = planOrder(); // redefined for each transaction model

        try {
            executionPlan.execute(); // independent of transactional model
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception e) {
            unitOfWorkService.abort(unitOfWork);
            throw e;
        }
    }
}
