package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.concurrent.CompletionException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

public abstract class WorkflowFunctionality {
    protected Workflow workflow;

    public void executeWorkflow(UnitOfWork unitOfWork) {
        try {
            workflow.execute(unitOfWork).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SimulatorException) {
                throw (SimulatorException) cause;
            } else {
                throw e;
            }
        } catch (SimulatorException e) {
            throw e;
        }
        
    }

    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(UnitOfWork unitOfWork) {
        try {
            workflow.resume(unitOfWork).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SimulatorException) {
                throw (SimulatorException) cause;
            } else {
                throw e;
            }
        } catch (SimulatorException e) {
            throw e;
        }
        
    }

    public int getWorkflowTotalDelay() {
        return workflow.getWorkflowTotalDelay();
    }
}