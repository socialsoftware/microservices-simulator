package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuizDto;

@Component
public class QuizSagaFunctionality extends WorkflowFunctionality {
private final QuizService quizService;
private final SagaUnitOfWorkService unitOfWorkService;

public QuizSagaFunctionality(QuizService quizService, SagaUnitOfWorkService
unitOfWorkService) {
this.quizService = quizService;
this.unitOfWorkService = unitOfWorkService;
}


}