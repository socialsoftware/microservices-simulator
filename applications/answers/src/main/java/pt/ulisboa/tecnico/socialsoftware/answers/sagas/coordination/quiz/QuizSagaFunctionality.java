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

    public void createQuiz(String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions, QuizExecution execution, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for createQuiz
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getQuizById(Integer quizId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getQuizById
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getAllQuizzes(SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAllQuizzes
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getQuizzesByExecution(Integer executionId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getQuizzesByExecution
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getQuizzesByType(String quizType, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getQuizzesByType
        // This method should orchestrate the saga workflow
        return null;
    }

    public void updateQuiz(Integer quizId, String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for updateQuiz
        // This method should orchestrate the saga workflow
        return null;
    }

    public void deleteQuiz(Integer quizId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for deleteQuiz
        // This method should orchestrate the saga workflow
        return null;
    }
}