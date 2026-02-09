package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionTopicFunctionalitySagas extends WorkflowFunctionality {
    private QuestionTopicDto updatedTopicDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateQuestionTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateTopicStep = new SagaSyncStep("updateTopicStep", () -> {
            QuestionTopicDto updatedTopicDto = questionService.updateQuestionTopic(questionId, topicAggregateId, topicDto, unitOfWork);
            setUpdatedTopicDto(updatedTopicDto);
        });

        workflow.addStep(updateTopicStep);
    }
    public QuestionTopicDto getUpdatedTopicDto() {
        return updatedTopicDto;
    }

    public void setUpdatedTopicDto(QuestionTopicDto updatedTopicDto) {
        this.updatedTopicDto = updatedTopicDto;
    }
}
