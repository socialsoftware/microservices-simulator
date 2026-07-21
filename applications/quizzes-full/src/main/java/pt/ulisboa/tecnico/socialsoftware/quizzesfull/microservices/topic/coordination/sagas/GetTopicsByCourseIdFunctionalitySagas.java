package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicsByCourseIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;

import java.util.List;

public class GetTopicsByCourseIdFunctionalitySagas extends WorkflowFunctionality {
    private List<TopicDto> topics;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTopicsByCourseIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                  Integer courseAggregateId,
                                                  SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            GetTopicsByCourseIdCommand cmd = new GetTopicsByCourseIdCommand(
                    unitOfWork, ServiceMapping.TOPIC.getServiceName(), courseAggregateId);
            this.topics = (List<TopicDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getTopicsStep);
    }

    public List<TopicDto> getTopics() {
        return topics;
    }
}
