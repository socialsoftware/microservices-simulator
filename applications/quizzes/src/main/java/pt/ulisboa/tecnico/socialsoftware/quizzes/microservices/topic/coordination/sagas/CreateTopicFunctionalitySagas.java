package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicCourse course;
    private TopicDto createdTopicDto;
    private CourseDto courseDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            getCourseByIdCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            this.courseDto = (CourseDto) commandGateway.send(getCourseByIdCommand);
            TopicCourse course = new TopicCourse(courseDto);
            this.setCourse(course);
        });

        getCourseStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseDto.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep createTopicStep = new SagaStep("createTopicStep", () -> {
            CreateTopicCommand createTopicCommand = new CreateTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto, this.getCourse());
            TopicDto createdTopicDto = (TopicDto) commandGateway.send(createTopicCommand);
            this.setCreatedTopicDto(createdTopicDto);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(createTopicStep);
    }

    public TopicCourse getCourse() {
        return course;
    }

    public void setCourse(TopicCourse course) {
        this.course = course;
    }

    public TopicDto getCreatedTopicDto() {
        return createdTopicDto;
    }

    public void setCreatedTopicDto(TopicDto createdTopicDto) {
        this.createdTopicDto = createdTopicDto;
    }
}