package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class QuizSagaCoordination extends WorkflowFunctionality {
private QuizDto quizDto;
private SagaQuizDto quiz;
private final QuizService quizService;
private final SagaUnitOfWorkService unitOfWorkService;

public QuizSagaCoordination(QuizService quizService, SagaUnitOfWorkService
unitOfWorkService,
QuizDto quizDto, SagaUnitOfWork unitOfWork) {
this.quizService = quizService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(quizDto, unitOfWork);
}

public void buildWorkflow(QuizDto quizDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public QuizDto getQuizDto() {
return quizDto;
}

public void setQuizDto(QuizDto quizDto) {
this.quizDto = quizDto;
}

public SagaQuizDto getQuiz() {
return quiz;
}

public void setQuiz(SagaQuizDto quiz) {
this.quiz = quiz;
}
}