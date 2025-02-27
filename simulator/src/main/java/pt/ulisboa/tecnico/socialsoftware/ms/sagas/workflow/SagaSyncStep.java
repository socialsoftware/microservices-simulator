package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;

public class SagaSyncStep extends SyncStep implements SagaStep {
    private Runnable compensationLogic;

    public SagaSyncStep(String stepName, Runnable syncOperation) {
        super(stepName, syncOperation);
    }

    public SagaSyncStep(String stepName, Runnable syncOperation, ArrayList<FlowStep> dependencies) {
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
        try {
            getSyncOperation().run();
            if (getCompensation() != null) {
                ((SagaUnitOfWork)unitOfWork).registerCompensation(getCompensation());
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw e;
            //return CompletableFuture.failedFuture(e);
        }
    }
}
