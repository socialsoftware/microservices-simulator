package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTopicsFunctionalitySagas extends WorkflowFunctionality {
    private List<TopicDto> topics;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllTopicsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TopicService topicService) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllTopicsStep = new SagaSyncStep("getAllTopicsStep", () -> {
            List<TopicDto> topics = topicService.getAllTopics();
            setTopics(topics);
        });

        workflow.addStep(getAllTopicsStep);

    }

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }
}
