package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuestionDto;

@Component
public class QuestionSagaFunctionality extends WorkflowFunctionality {
private final QuestionService questionService;
private final SagaUnitOfWorkService unitOfWorkService;

public QuestionSagaFunctionality(QuestionService questionService, SagaUnitOfWorkService
unitOfWorkService) {
this.questionService = questionService;
this.unitOfWorkService = unitOfWorkService;
}


}