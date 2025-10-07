package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.answer;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaAnswerDto;

@Component
public class AnswerSagaFunctionality extends WorkflowFunctionality {
private final AnswerService answerService;
private final SagaUnitOfWorkService unitOfWorkService;

public AnswerSagaFunctionality(AnswerService answerService, SagaUnitOfWorkService
unitOfWorkService) {
this.answerService = answerService;
this.unitOfWorkService = unitOfWorkService;
}


}