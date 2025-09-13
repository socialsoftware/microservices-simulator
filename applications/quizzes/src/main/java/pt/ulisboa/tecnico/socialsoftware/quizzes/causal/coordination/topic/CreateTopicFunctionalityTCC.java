package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

public class CreateTopicFunctionalityTCC extends WorkflowFunctionality {
    private TopicCourse course;
    private TopicDto createdTopicDto;
    @SuppressWarnings("unused")
    private final TopicService topicService;
    @SuppressWarnings("unused")
    private final CourseService courseService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTopicFunctionalityTCC(TopicService topicService, CourseService courseService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseAggregateId, TopicDto topicDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // CourseDto courseDto = courseService.getCourseById(courseAggregateId, unitOfWork);
            GetCourseByIdCommand GetCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            CourseDto courseDto = (CourseDto) commandGateway.send(GetCourseByIdCommand);
            TopicCourse course = new TopicCourse(courseDto);
            // this.createdTopicDto = topicService.createTopic(topicDto, course, unitOfWork);
            CreateTopicCommand CreateTopicCommand = new CreateTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto, course);
            this.createdTopicDto = (TopicDto) commandGateway.send(CreateTopicCommand);
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