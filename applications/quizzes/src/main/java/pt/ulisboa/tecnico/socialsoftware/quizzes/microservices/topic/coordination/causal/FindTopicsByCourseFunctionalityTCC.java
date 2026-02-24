package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.FindTopicsByCourseIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.util.List;

public class FindTopicsByCourseFunctionalityTCC extends WorkflowFunctionality {
    private List<TopicDto> topics;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindTopicsByCourseFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                              Integer courseAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            FindTopicsByCourseIdCommand FindTopicsByCourseIdCommand = new FindTopicsByCourseIdCommand(unitOfWork,
                    ServiceMapping.TOPIC.getServiceName(), courseAggregateId);
            this.topics = (List<TopicDto>) commandGateway.send(FindTopicsByCourseIdCommand);
        });

        workflow.addStep(step);
    }

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }
}