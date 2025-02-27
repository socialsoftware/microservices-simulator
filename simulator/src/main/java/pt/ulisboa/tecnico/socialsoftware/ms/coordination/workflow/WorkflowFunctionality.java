package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.concurrent.CompletionException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

public abstract class WorkflowFunctionality {
    protected Workflow workflow;

    public void executeWorkflow(UnitOfWork unitOfWork) {
        try {
            workflow.execute(unitOfWork).join();
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

    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(UnitOfWork unitOfWork) {
        try {
            workflow.resume(unitOfWork).join();
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
}