package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.question;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.CourseSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TopicSagaState;

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

    private final QuestionService questionService;
    private final TopicService topicService;
    private final CourseService courseService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuestionFunctionalitySagas(QuestionService questionService, TopicService topicService, CourseService courseService, SagaUnitOfWorkService unitOfWorkService,
                                            Integer courseAggregateId, QuestionDto questionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.questionService = questionService;
        this.topicService = topicService;
        this.courseService = courseService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep validateQuestionTopicsStep = new SagaSyncStep("validateQuestionTopicsStep", () -> {
            for (TopicDto topicDto : questionDto.getTopicDto()) {
                if (!topicDto.getCourseId().equals(courseAggregateId)) {
                    throw new QuizzesException(QuizzesErrorMessage.QUESTION_TOPIC_INVALID_COURSE, topicDto.getAggregateId(), courseAggregateId);
                }
            }
        });
    
        SagaSyncStep getCourseStep = new SagaSyncStep("getCourseStep", () -> {
//            CourseDto course = courseService.getCourseById(courseAggregateId, unitOfWork);
//            this.courseDto = course;
//            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), CourseSagaState.READ_COURSE, unitOfWork);
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            getCourseByIdCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            CourseDto course = (CourseDto) commandGateway.send(getCourseByIdCommand);
            this.courseDto = course;

            QuestionCourse questionCourse = new QuestionCourse(course);
            this.setCourse(questionCourse);
        });

        getCourseStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(courseDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Logger.getLogger(CreateQuestionFunctionalitySagas.class.getName()).info("Compensating getCourseStep");
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseDto.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep getTopicsStep = new SagaSyncStep("getTopicsStep", () -> {
            List<TopicDto> topics = questionDto.getTopicDto().stream()
                .map(topicDto -> {
//                    TopicDto topic = (TopicDto) topicService.getTopicById(topicDto.getAggregateId(), unitOfWork);
//                    unitOfWorkService.registerSagaState(topic.getAggregateId(), TopicSagaState.READ_TOPIC, unitOfWork);
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
//                unitOfWorkService.registerSagaState(topicDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
                Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
                command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                commandGateway.send(command);
            });
        }, unitOfWork);
    
        SagaSyncStep createQuestionStep = new SagaSyncStep("createQuestionStep", () -> {
//            QuestionDto createdQuestion = questionService.createQuestion(this.getCourse(), questionDto, this.getTopics(), unitOfWork);
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