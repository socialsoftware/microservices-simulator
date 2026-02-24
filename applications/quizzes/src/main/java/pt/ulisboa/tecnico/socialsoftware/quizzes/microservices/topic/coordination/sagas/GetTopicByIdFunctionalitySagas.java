package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

public class GetTopicByIdFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public GetTopicByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                          Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            TopicDto topicDto = (TopicDto) CommandGateway.send(getTopicByIdCommand);
            this.setTopicDto(topicDto);
        });

        workflow.addStep(getTopicStep);
    }

    public void setTopicDto(TopicDto topicDto) {
        this.topicDto = topicDto;
    }

    public TopicDto getTopicDto() {
        return this.topicDto;
    }

}
