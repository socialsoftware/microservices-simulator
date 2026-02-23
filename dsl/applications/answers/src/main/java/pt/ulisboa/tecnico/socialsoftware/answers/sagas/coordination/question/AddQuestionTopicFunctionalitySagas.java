package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddQuestionTopicFunctionalitySagas extends WorkflowFunctionality {
    private QuestionTopicDto addedTopicDto;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddQuestionTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addTopicStep = new SagaSyncStep("addTopicStep", () -> {
            QuestionTopicDto addedTopicDto = questionService.addQuestionTopic(questionId, topicAggregateId, topicDto, unitOfWork);
            setAddedTopicDto(addedTopicDto);
        });

        workflow.addStep(addTopicStep);
    }
    public QuestionTopicDto getAddedTopicDto() {
        return addedTopicDto;
    }

    public void setAddedTopicDto(QuestionTopicDto addedTopicDto) {
        this.addedTopicDto = addedTopicDto;
    }
}
