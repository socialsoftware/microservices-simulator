package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topicDto;

    public CreateTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TopicDto topicDto,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, topicDto, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, TopicDto topicDto,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: the fetch itself is the check — it throws when the course does not
        // exist, so no explicit guard is written in TopicService. Topic caches no Course field
        // beyond courseAggregateId, so the fetched DTO is not carried forward.
        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand cmd = new GetCourseByIdCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), topicDto.getCourseAggregateId());
            commandGateway.send(cmd);
        });

        SagaStep createTopicStep = new SagaStep("createTopicStep", () -> {
            CreateTopicCommand cmd = new CreateTopicCommand(
                    unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto);
            this.topicDto = (TopicDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        this.workflow.addStep(getCourseStep);
        this.workflow.addStep(createTopicStep);
    }

    public TopicDto getTopicDto() {
        return topicDto;
    }
}
