package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetQuestionTopicFunctionalitySagas extends WorkflowFunctionality {
    private QuestionTopicDto topicDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetQuestionTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, Integer topicAggregateId) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
            QuestionTopicDto topicDto = questionService.getQuestionTopic(questionId, topicAggregateId, unitOfWork);
            setTopicDto(topicDto);
        });

        workflow.addStep(getTopicStep);
    }
    public QuestionTopicDto getTopicDto() {
        return topicDto;
    }

    public void setTopicDto(QuestionTopicDto topicDto) {
        this.topicDto = topicDto;
    }
}
