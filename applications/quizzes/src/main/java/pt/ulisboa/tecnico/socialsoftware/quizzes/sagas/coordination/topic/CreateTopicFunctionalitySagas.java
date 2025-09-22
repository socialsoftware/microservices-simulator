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
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.CourseSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicCourse course;
    private TopicDto createdTopicDto;
    private CourseDto courseDto;
    private final TopicService topicService;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public CreateTopicFunctionalitySagas(TopicService topicService, CourseService courseService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseStep = new SagaSyncStep("getCourseStep", () -> {
            // this.courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            // unitOfWorkService.registerSagaState(courseDto.getAggregateId(),
            // CourseSagaState.READ_COURSE, unitOfWork);
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork,
                    ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            getCourseByIdCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            this.courseDto = (CourseDto) CommandGateway.send(getCourseByIdCommand);
            TopicCourse course = new TopicCourse(courseDto);
            this.setCourse(course);
        });

        getCourseStep.registerCompensation(() -> {
            // unitOfWorkService.registerSagaState(courseDto.getAggregateId(),
            // GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(),
                    courseDto.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            CommandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep createTopicStep = new SagaSyncStep("createTopicStep", () -> {
            // TopicDto createdTopicDto = topicService.createTopic(topicDto,
            // this.getCourse(), unitOfWork);
            CreateTopicCommand createTopicCommand = new CreateTopicCommand(unitOfWork,
                    ServiceMapping.TOPIC.getServiceName(), topicDto, this.getCourse());
            TopicDto createdTopicDto = (TopicDto) CommandGateway.send(createTopicCommand);
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