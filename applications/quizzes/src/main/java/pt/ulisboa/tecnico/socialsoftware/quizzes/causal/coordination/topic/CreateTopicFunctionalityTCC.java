package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

public class CreateTopicFunctionalityTCC extends WorkflowFunctionality {
    private TopicCourse course;
    private TopicDto createdTopicDto;
    private final TopicService topicService;
    private final CourseService courseService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public CreateTopicFunctionalityTCC(TopicService topicService, CourseService courseService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, TopicDto topicDto, CausalUnitOfWork unitOfWork) {
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            TopicCourse course = new TopicCourse(courseDto);
            this.createdTopicDto = topicService.createTopic(topicDto, course, unitOfWork);
        });
    
        workflow.addStep(step);
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