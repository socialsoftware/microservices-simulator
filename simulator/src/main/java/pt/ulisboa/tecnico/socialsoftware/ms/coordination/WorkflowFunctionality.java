package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.concurrent.CompletionException;

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
        try {
            workflow.executeUntilStep(stepName, unitOfWork);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SimulatorException) {
                throw (SimulatorException) cause;
            } else {
                throw e;
            }
        }
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

    public void compensateUntilStep(String stepName, UnitOfWork unitOfWork) {
        try {
            workflow.compensateUntilStep(stepName, unitOfWork);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SimulatorException) {
                throw (SimulatorException) cause;
            } else {
                throw e;
            }
        }
    }

    public void resumeCompensation(UnitOfWork unitOfWork) {
        try {
            workflow.resumeCompensation(unitOfWork);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SimulatorException) {
                throw (SimulatorException) cause;
            } else {
                throw e;
            }
        }
    }

    public int getWorkflowTotalDelay() {
        return workflow.getWorkflowTotalDelay();
    }
}