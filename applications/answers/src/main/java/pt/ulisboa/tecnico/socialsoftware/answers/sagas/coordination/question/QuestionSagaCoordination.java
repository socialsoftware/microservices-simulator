package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import ${this.getBasePackage()}.ms.coordination.workflow.WorkflowFunctionality;
import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWork;
import ${this.getBasePackage()}.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import ${this.getBasePackage()}.ms.sagas.workflow.SagaSyncStep;
import ${this.getBasePackage()}.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.QuestionSagaState;
import ${this.getBasePackage()}.ms.sagas.aggregate.GenericSagaState;

public class QuestionSagaCoordination extends WorkflowFunctionality {
private QuestionDto questionDto;
private SagaQuestionDto question;
private final QuestionService questionService;
private final SagaUnitOfWorkService unitOfWorkService;

public QuestionSagaCoordination(QuestionService questionService, SagaUnitOfWorkService
unitOfWorkService,
QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
this.questionService = questionService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(questionDto, unitOfWork);
}

public void buildWorkflow(QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public QuestionDto getQuestionDto() {
return questionDto;
}

public void setQuestionDto(QuestionDto questionDto) {
this.questionDto = questionDto;
}

public SagaQuestionDto getQuestion() {
return question;
}

public void setQuestion(SagaQuestionDto question) {
this.question = question;
}
}