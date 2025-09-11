package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

public class GetTopicByIdFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topicDto;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTopicByIdFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,  
                                Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
//            TopicDto topicDto = topicService.getTopicById(topicAggregateId, unitOfWork);
            GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            TopicDto topicDto = (TopicDto) commandGateway.send(getTopicByIdCommand);
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
