package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.concurrent.CompletionException;

import org.codehaus.groovy.runtime.dgmimpl.arrays.BooleanArrayGetAtMetaMethod;

import groovyjarjarantlr4.v4.codegen.model.ExceptionClause;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

public abstract class WorkflowFunctionality {
    protected Workflow workflow;
    public Boolean crashed = false;

      // Method to check if crashed flag is true
      private void checkIfCrashed() {
        if (crashed) {
            throw new IllegalStateException("Workflow has crashed and cannot be executed further.");
        }
    }

    public void executeWorkflow(UnitOfWork unitOfWork) {
        try {
            checkIfCrashed();
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
            checkIfCrashed();
            workflow.executeUntilStep(stepName, unitOfWork);
        } catch (Exception e) {
            throw e;
        }
    }

    public void resumeWorkflow(UnitOfWork unitOfWork) {
        try {
            checkIfCrashed();
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
        } catch (Exception e) {
            throw e;
        }
        
    }

    public void executeUntilErrorWithRecovery(String stepName, UnitOfWork unitOfWork){
        try {
            checkIfCrashed();
            workflow.executeUntilErrorWithRecovery(stepName,unitOfWork);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TutorException) {
                throw (TutorException) cause;
            } else {
                throw e;
            }
        } catch (TutorException e) {
            throw e;
        }
        
    }
    public void executeUntilError(String stepName, UnitOfWork unitOfWork){
        try {
            checkIfCrashed();
            crashed = true;
            workflow.executeUntilErrorWithRecovery(stepName,unitOfWork);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof TutorException) {
                throw (TutorException) cause;
            } else {
                throw e;
            }
        } catch (TutorException e) {
            throw e;
        }
        
    }
    public void executeWithOmission(UnitOfWork unitOfWork){
        try {
            checkIfCrashed();
            workflow.executeWithOmission(unitOfWork);
        } catch (Exception e) {
            throw e;
        }
    }

    public void compensate(UnitOfWork unitOfWork){
        crashed = false;
        workflow.compensate(unitOfWork);
    }
}