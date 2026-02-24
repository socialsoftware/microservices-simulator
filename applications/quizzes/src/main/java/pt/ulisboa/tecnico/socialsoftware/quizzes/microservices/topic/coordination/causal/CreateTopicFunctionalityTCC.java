package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

public class CreateTopicFunctionalityTCC extends WorkflowFunctionality {
    private TopicCourse course;
    private TopicDto createdTopicDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTopicFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                       Integer courseAggregateId, TopicDto topicDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, TopicDto topicDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetCourseByIdCommand GetCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            CourseDto courseDto = (CourseDto) commandGateway.send(GetCourseByIdCommand);
            TopicCourse course = new TopicCourse(courseDto);
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