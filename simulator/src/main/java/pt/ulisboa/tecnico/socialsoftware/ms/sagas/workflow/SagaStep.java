package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

// TODO this will become SagaStep
public class SagaStep extends Step {
    private Runnable compensationLogic;

    public SagaStep(String stepName, Runnable syncOperation) {
        super(stepName, syncOperation);
    }

    public SagaStep(String stepName, Runnable syncOperation, ArrayList<FlowStep> dependencies) {
        super(stepName, syncOperation, dependencies);
    }

    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork) {
        this.compensationLogic = compensationLogic;
    }

    public Runnable getCompensation() {
        return this.compensationLogic;
    }

    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        getSyncOperation().run();
        if (getCompensation() != null) {
            ((SagaUnitOfWork)unitOfWork).registerCompensation(getCompensation());
        }
        return CompletableFuture.completedFuture(null);
    }
}
