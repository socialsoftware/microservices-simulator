package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.UpdateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topic;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTopicFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,  
                            TopicDto topicDto, TopicFactory topicFactory, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicDto, topicFactory, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, TopicFactory topicFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
//            TopicDto topic = (TopicDto) topicService.getTopicById(topicDto.getAggregateId(), unitOfWork);
//            unitOfWorkService.registerSagaState(topic.getAggregateId(), TopicSagaState.READ_TOPIC, unitOfWork);
            GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
            getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
            TopicDto topic = (TopicDto) commandGateway.send(getTopicByIdCommand);
            this.setTopic(topic);
        });
    
        getTopicStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(topic.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topic.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep updateTopicStep = new SagaSyncStep("updateTopicStep", () -> {
//            topicService.updateTopic(topicDto, unitOfWork);
            UpdateTopicCommand updateTopicCommand = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto);
            commandGateway.send(updateTopicCommand);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));
    
        workflow.addStep(getTopicStep);
        workflow.addStep(updateTopicStep);
    }
    

    public TopicDto getTopic() {
        return topic;
    }

    public void setTopic(TopicDto topic) {
        this.topic = topic;
    }
}