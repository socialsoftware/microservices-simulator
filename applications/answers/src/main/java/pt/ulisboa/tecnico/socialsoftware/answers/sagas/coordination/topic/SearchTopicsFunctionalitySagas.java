package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class SearchTopicsFunctionalitySagas extends WorkflowFunctionality {
    private List<TopicDto> searchedTopicDtos;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public SearchTopicsFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService, String name, Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(name, courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(String name, Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchTopicsStep = new SagaSyncStep("searchTopicsStep", () -> {
            List<TopicDto> searchedTopicDtos = topicService.searchTopics(name, courseAggregateId, unitOfWork);
            setSearchedTopicDtos(searchedTopicDtos);
        });

        workflow.addStep(searchTopicsStep);
    }

    public List<TopicDto> getSearchedTopicDtos() {
        return searchedTopicDtos;
    }

    public void setSearchedTopicDtos(List<TopicDto> searchedTopicDtos) {
        this.searchedTopicDtos = searchedTopicDtos;
    }
}
