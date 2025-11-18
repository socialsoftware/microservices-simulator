package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    

    private final QuizService quizService;
    private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final SagaUnitOfWork unitOfWork;

    public UpdateQuizFunctionalitySagas(QuizService quizService, SagaUnitOfWorkService sagaUnitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.quizService = quizService;
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.unitOfWork = unitOfWork;
    }

    public void buildWorkflow() {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);

    }

}


