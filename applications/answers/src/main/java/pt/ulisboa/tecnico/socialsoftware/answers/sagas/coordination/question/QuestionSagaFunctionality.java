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

    public void createQuestion(String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, QuestionCourse course, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for createQuestion
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getQuestionById(Integer questionId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getQuestionById
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getAllQuestions(SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAllQuestions
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getQuestionsByCourse(Integer courseId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getQuestionsByCourse
        // This method should orchestrate the saga workflow
        return null;
    }

    public void getQuestionsByTopic(Integer topicId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getQuestionsByTopic
        // This method should orchestrate the saga workflow
        return null;
    }

    public void updateQuestion(Integer questionId, String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for updateQuestion
        // This method should orchestrate the saga workflow
        return null;
    }

    public void deleteQuestion(Integer questionId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for deleteQuestion
        // This method should orchestrate the saga workflow
        return null;
    }
}