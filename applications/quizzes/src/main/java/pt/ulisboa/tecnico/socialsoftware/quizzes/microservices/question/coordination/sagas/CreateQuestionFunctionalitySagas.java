package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.UpdateCourseQuestionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.CreateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.states.CourseSagaState;
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

        SagaStep getCourseStep = new SagaStep("getCourseStep", () -> {
            GetCourseByIdCommand getCourseByIdCommand = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCourseByIdCommand);
            sagaCommand.setSemanticLock(CourseSagaState.READ_COURSE);
            CourseDto course = (CourseDto) commandGateway.send(sagaCommand);
            this.courseDto = course;

            QuestionCourse questionCourse = new QuestionCourse(course);
            this.setCourse(questionCourse);
        });

        getCourseStep.registerCompensation(() -> {
            Logger.getLogger(CreateQuestionFunctionalitySagas.class.getName()).info("Compensating getCourseStep");
            Command command = new Command(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseDto.getAggregateId());
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            List<TopicDto> topics = questionDto.getTopicDto().stream()
                    .map(topicDto -> {
                        GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
                        SagaCommand sagaCommand = new SagaCommand(getTopicByIdCommand);
                        sagaCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
                        TopicDto fetchedTopic = (TopicDto) commandGateway.send(sagaCommand);
                        return fetchedTopic;
                    })
                    .collect(Collectors.toList());
            this.setTopics(topics);
        });

        getTopicsStep.registerCompensation(() -> {
            Logger.getLogger(CreateQuestionFunctionalitySagas.class.getName()).info("Compensating getTopicsStep");
            questionDto.getTopicDto().forEach(topicDto -> {
                Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
                SagaCommand sagaCommand = new SagaCommand(command);
                sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                commandGateway.send(sagaCommand);
            });
        }, unitOfWork);

        SagaStep createQuestionStep = new SagaStep("createQuestionStep", () -> {
            CreateQuestionCommand createQuestionCommand = new CreateQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), this.getCourse(), questionDto, this.getTopics());
            QuestionDto createdQuestion = (QuestionDto) commandGateway.send(createQuestionCommand);
            this.setCreatedQuestion(createdQuestion);
        }, new ArrayList<>(Arrays.asList(getCourseStep, getTopicsStep)));

        // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
        SagaStep updateCourseQuestionCountStep = new SagaStep("updateCourseQuestionCountStep", () -> {
            UpdateCourseQuestionCountCommand cmd = new UpdateCourseQuestionCountCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), this.courseDto.getAggregateId(), true);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(createQuestionStep)));

        workflow.addStep(getCourseStep);
        workflow.addStep(getTopicsStep);
        workflow.addStep(createQuestionStep);
        workflow.addStep(updateCourseQuestionCountStep);
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
