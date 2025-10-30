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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.util.List;
import java.util.stream.Collectors;

public class CreateQuestionFunctionalityTCC extends WorkflowFunctionality {
    private QuestionCourse course;
    private List<TopicDto> topics;
    private QuestionDto createdQuestion;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuestionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                          Integer courseAggregateId, QuestionDto questionDto, CausalUnitOfWork unitOfWork,
                                          CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, QuestionDto questionDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
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

            List<TopicDto> topics = questionDto.getTopicDto().stream()
                    .map(topicDto -> (TopicDto) commandGateway.send(new GetTopicByIdCommand(unitOfWork,
                            ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId())))
                    .collect(Collectors.toList());

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