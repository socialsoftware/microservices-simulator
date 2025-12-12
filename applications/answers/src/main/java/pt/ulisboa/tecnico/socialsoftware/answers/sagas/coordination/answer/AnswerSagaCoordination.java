package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.AnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;

public class AnswerSagaCoordination extends WorkflowFunctionality {
private AnswerDto answerDto;
private SagaAnswerDto answer;
private final AnswerService answerService;
private final SagaUnitOfWorkService unitOfWorkService;

public AnswerSagaCoordination(AnswerService answerService, SagaUnitOfWorkService
unitOfWorkService,
AnswerDto answerDto, SagaUnitOfWork unitOfWork) {
this.answerService = answerService;
this.unitOfWorkService = unitOfWorkService;
this.buildWorkflow(answerDto, unitOfWork);
}

public void buildWorkflow(AnswerDto answerDto, SagaUnitOfWork unitOfWork) {
this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
// Saga coordination logic will be implemented here
}

// Getters and setters
public AnswerDto getAnswerDto() {
return answerDto;
}

public void setAnswerDto(AnswerDto answerDto) {
this.answerDto = answerDto;
}

public SagaAnswerDto getAnswer() {
return answer;
}

public void setAnswer(SagaAnswerDto answer) {
this.answer = answer;
}
}