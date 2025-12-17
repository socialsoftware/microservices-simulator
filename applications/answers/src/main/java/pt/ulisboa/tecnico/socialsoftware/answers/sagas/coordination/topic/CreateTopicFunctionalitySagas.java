package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto createdTopicDto;
    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private TopicCourse course;
    private SagaCourseDto courseDto;
    private final CourseService courseService;


    public CreateTopicFunctionalitySagas(TopicService topicService, CourseService courseService, Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.courseService = courseService;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseStep = new SagaSyncStep("getCourseStep", () -> {
            courseDto = (SagaCourseDto) courseService.getCourseById(courseAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), CourseSagaState.READ_COURSE, unitOfWork);
            TopicCourse course = new TopicCourse(courseDto);
            setCourse(course);
        });

        getCourseStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
        SagaSyncStep createTopicStep = new SagaSyncStep("createTopicStep", () -> {
            TopicDto createdTopicDto = topicService.createTopic(getCourse(), topicDto, unitOfWork);
            setCreatedTopicDto(createdTopicDto);
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
