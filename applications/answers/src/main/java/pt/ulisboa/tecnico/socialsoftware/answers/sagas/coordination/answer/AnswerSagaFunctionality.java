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

    public Object createAnswer(Object student, Object courseExecution, Object quiz, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for createAnswer
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getAnswerById(Integer answerId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAnswerById
        // This method should orchestrate the saga workflow
        return null;
    }

    public List<Answer> getAnswersByStudent(Integer studentId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAnswersByStudent
        // This method should orchestrate the saga workflow
        return null;
    }

    public List<Answer> getAnswersByQuiz(Integer quizId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAnswersByQuiz
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object submitAnswer(Integer answerId, Integer questionId, String answer, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for submitAnswer
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object completeAnswer(Integer answerId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for completeAnswer
        // This method should orchestrate the saga workflow
        return null;
    }
}