package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateTopicFunctionality extends WorkflowFunctionality {
    private TopicCourse course;
    private TopicDto createdTopicDto;

    private SagaWorkflow workflow;

    private final TopicService topicService;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateTopicFunctionality(TopicService topicService, CourseService courseService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getCourseStep = new SyncStep("getCourseStep", () -> {
            CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            TopicCourse course = new TopicCourse(courseDto);
            this.setCourse(course);
        });
    
        SyncStep createTopicStep = new SyncStep("createTopicStep", () -> {
            TopicDto createdTopicDto = topicService.createTopic(topicDto, this.getCourse(), unitOfWork);
            this.setCreatedTopicDto(createdTopicDto);
        }, new ArrayList<>(Arrays.asList(getCourseStep)));
    
        createTopicStep.registerCompensation(() -> {
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(this.getCreatedTopicDto().getAggregateId(), unitOfWork);
            topic.remove();
            unitOfWork.registerChanged(topic);
        }, unitOfWork);
    
        workflow.addStep(getCourseStep);
        workflow.addStep(createTopicStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
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