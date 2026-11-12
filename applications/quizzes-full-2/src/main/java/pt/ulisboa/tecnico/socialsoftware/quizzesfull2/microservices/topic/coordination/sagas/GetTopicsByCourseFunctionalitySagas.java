package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicsByCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;

import java.util.List;

public class GetTopicsByCourseFunctionalitySagas extends WorkflowFunctionality {
    private List<TopicDto> topics;

    public GetTopicsByCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer courseAggregateId,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer courseAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicsByCourseStep = new SagaStep("getTopicsByCourseStep", () -> {
            GetTopicsByCourseCommand cmd = new GetTopicsByCourseCommand(
                    unitOfWork, ServiceMapping.TOPIC.getServiceName(), courseAggregateId);
            this.topics = (List<TopicDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getTopicsByCourseStep);
    }

    public List<TopicDto> getTopics() {
        return topics;
    }
}
