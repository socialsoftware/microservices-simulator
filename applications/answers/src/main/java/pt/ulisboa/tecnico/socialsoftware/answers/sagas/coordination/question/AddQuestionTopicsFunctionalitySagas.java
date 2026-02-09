package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddQuestionTopicsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionTopicDto> addedTopicDtos;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddQuestionTopicsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuestionService questionService, Integer questionId, List<QuestionTopicDto> topicDtos) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(questionId, topicDtos, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, List<QuestionTopicDto> topicDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addTopicsStep = new SagaSyncStep("addTopicsStep", () -> {
            List<QuestionTopicDto> addedTopicDtos = questionService.addQuestionTopics(questionId, topicDtos, unitOfWork);
            setAddedTopicDtos(addedTopicDtos);
        });

        workflow.addStep(addTopicsStep);
    }
    public List<QuestionTopicDto> getAddedTopicDtos() {
        return addedTopicDtos;
    }

    public void setAddedTopicDtos(List<QuestionTopicDto> addedTopicDtos) {
        this.addedTopicDtos = addedTopicDtos;
    }
}
