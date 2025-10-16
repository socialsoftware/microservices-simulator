package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

import java.util.List;
import java.util.stream.Collectors;

public class CreateQuestionFunctionalityTCC extends WorkflowFunctionality {
    private QuestionCourse course;
    private List<TopicDto> topics;
    private QuestionDto createdQuestion;
    @SuppressWarnings("unused")
    private final QuestionService questionService;
    @SuppressWarnings("unused")
    private final TopicService topicService;
    @SuppressWarnings("unused")
    private final CourseService courseService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuestionFunctionalityTCC(QuestionService questionService, TopicService topicService,
            CourseService courseService, CausalUnitOfWorkService unitOfWorkService,
            Integer courseAggregateId, QuestionDto questionDto, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.questionService = questionService;
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // QuestionCourse course = new
            // QuestionCourse(courseService.getCourseById(courseAggregateId, unitOfWork));
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork,
                    ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            QuestionCourse course = new QuestionCourse((CourseDto) commandGateway.send(getCourseByIdCommand));
            /*
             * COURSE_SAME_TOPICS_COURSE
             */

            for (TopicDto topicDto : questionDto.getTopicDto()) {
                if (!topicDto.getCourseId().equals(courseAggregateId)) {
                    throw new QuizzesException(QuizzesErrorMessage.QUESTION_TOPIC_INVALID_COURSE,
                            topicDto.getAggregateId(), courseAggregateId);
                }
            }

            // List<TopicDto> topics = questionDto.getTopicDto().stream()
            // .map(topicDto -> topicService.getTopicById(topicDto.getAggregateId(),
            // unitOfWork))
            // .collect(Collectors.toList());
            List<TopicDto> topics = questionDto.getTopicDto().stream()
                    .map(topicDto -> (TopicDto) commandGateway.send(new GetTopicByIdCommand(unitOfWork,
                            ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId())))
                    .collect(Collectors.toList());

            // this.createdQuestion = questionService.createQuestion(course, questionDto,
            // topics, unitOfWork);
            CreateQuestionCommand createQuestionCommand = new CreateQuestionCommand(unitOfWork,
                    ServiceMapping.QUESTION.getServiceName(), course, questionDto, topics);
            this.createdQuestion = (QuestionDto) commandGateway.send(createQuestionCommand);
        });
        workflow.addStep(step);
    }

    public QuestionCourse getCourse() {
        return course;
    }

    public void setCourse(QuestionCourse course) {
        this.course = course;
    }

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }

    public QuestionDto getCreatedQuestion() {
        return createdQuestion;
    }

    public void setCreatedQuestion(QuestionDto createdQuestion) {
        this.createdQuestion = createdQuestion;
    }
}