package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.UpdateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

public class UpdateTopicFunctionalityTCC extends WorkflowFunctionality {
    private Topic oldTopic;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTopicFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                       TopicDto topicDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicDto, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            UpdateTopicCommand UpdateTopicCommand = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto);
            commandGateway.send(UpdateTopicCommand);
        });

        workflow.addStep(step);
    }

    public Topic getOldTopic() {
        return oldTopic;
    }

    public void setOldTopic(Topic oldTopic) {
        this.oldTopic = oldTopic;
    }
}