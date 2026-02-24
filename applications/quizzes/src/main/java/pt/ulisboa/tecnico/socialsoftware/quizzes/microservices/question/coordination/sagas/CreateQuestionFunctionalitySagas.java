package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CreateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionCourse course;
    private List<TopicDto> topics;
    private QuestionDto createdQuestion;
    private CourseDto courseDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer courseAggregateId, QuestionDto questionDto, SagaUnitOfWork unitOfWork,
                                            CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(courseAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep validateQuestionTopicsStep = new SagaStep("validateQuestionTopicsStep", () -> { // TODO
            for (TopicDto topicDto : questionDto.getTopicDto()) {
                if (!topicDto.getCourseId().equals(courseAggregateId)) {
                    throw new QuizzesException(QuizzesErrorMessage.QUESTION_TOPIC_INVALID_COURSE, topicDto.getAggregateId(), courseAggregateId);
                }
            }
        });

        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            getCourseByIdCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            CourseDto course = (CourseDto) commandGateway.send(getCourseByIdCommand);
            this.courseDto = course;

            QuestionCourse questionCourse = new QuestionCourse(course);
            this.setCourse(questionCourse);
        });

        getCourseStep.registerCompensation(() -> {
            Logger.getLogger(CreateQuestionFunctionalitySagas.class.getName()).info("Compensating getCourseStep");
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseDto.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            List<TopicDto> topics = questionDto.getTopicDto().stream()
                    .map(topicDto -> { // TODO
                        GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
                        getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
                        return (TopicDto) commandGateway.send(getTopicByIdCommand);
                    })
                    .collect(Collectors.toList());
            this.setTopics(topics);
        });

        getTopicsStep.registerCompensation(() -> {
            Logger.getLogger(CreateQuestionFunctionalitySagas.class.getName()).info("Compensating getTopicsStep");
            questionDto.getTopicDto().forEach(topicDto -> {
                Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
                command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                commandGateway.send(command);
            });
        }, unitOfWork);

        SagaStep createQuestionStep = new SagaStep("createQuestionStep", () -> {
            CreateQuestionCommand createQuestionCommand = new CreateQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), this.getCourse(), questionDto, this.getTopics());
            QuestionDto createdQuestion = (QuestionDto) commandGateway.send(createQuestionCommand);
            this.setCreatedQuestion(createdQuestion);
        }, new ArrayList<>(Arrays.asList(validateQuestionTopicsStep, getCourseStep, getTopicsStep)));

        workflow.addStep(validateQuestionTopicsStep);
        workflow.addStep(getCourseStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(createQuestionStep);
    }

    public QuestionCourse getCourse() {
        return course;
    }

    public void setCourse(QuestionCourse course) {
        this.course = course;
    }

    public List<TopicDto> getTopics() {
        return topics.stream()
                .map(TopicDto -> (TopicDto) TopicDto)
                .collect(Collectors.toList());
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