package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.question.GetQuestionsByCourseExecutionIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto executionDto;
    private UserDto creatorUserDto;
    private List<TopicDto> topicDtos;
    private Set<QuizQuestion> quizQuestions;
    private QuizDto createdQuizDto;
    private TournamentDto createdTournamentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer executionId, Integer creatorId,
                                               List<Integer> topicIds, Integer numberOfQuestions,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionId, creatorId, topicIds, numberOfQuestions, startTime, endTime, unitOfWork);
    }

    public void buildWorkflow(Integer executionId, Integer creatorId,
                               List<Integer> topicIds, Integer numberOfQuestions,
                               LocalDateTime startTime, LocalDateTime endTime,
                               SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        this.topicDtos = new ArrayList<>();

        SagaStep validateDatesStep = new SagaStep("validateDatesStep", () -> {
            if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
                throw new QuizzesFullException(
                        pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_START_BEFORE_END_TIME);
            }
        });

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId);
            this.executionDto = (ExecutionDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(validateDatesStep)));

        SagaStep getStudentStep = new SagaStep("getStudentStep", () -> {
            // P4a: throws COURSE_EXECUTION_STUDENT_NOT_FOUND if creator not enrolled
            GetStudentByExecutionIdAndUserIdCommand cmd = new GetStudentByExecutionIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId, creatorId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        SagaStep getCreatorUserStep = new SagaStep("getCreatorUserStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), creatorId);
            this.creatorUserDto = (UserDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getStudentStep)));

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            for (Integer topicId : topicIds) {
                GetTopicByIdCommand cmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
                this.topicDtos.add(topicDto);
            }
        }, new ArrayList<>(Arrays.asList(getCreatorUserStep)));

        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            GetQuestionsByCourseExecutionIdCommand cmd = new GetQuestionsByCourseExecutionIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    this.executionDto.getCourseId());
            @SuppressWarnings("unchecked")
            List<QuestionDto> allQuestions = (List<QuestionDto>) commandGateway.send(cmd);

            Set<Integer> topicIdSet = new HashSet<>(topicIds);
            this.quizQuestions = new HashSet<>();
            for (QuestionDto q : allQuestions) {
                if (this.quizQuestions.size() >= numberOfQuestions) break;
                boolean matchesTopic = q.getTopicIds().stream().anyMatch(topicIdSet::contains);
                if (matchesTopic) {
                    this.quizQuestions.add(new QuizQuestion(q));
                }
            }
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        SagaStep createQuizStep = new SagaStep("createQuizStep", () -> {
            QuizExecution quizExecution = new QuizExecution(this.executionDto);
            CreateQuizCommand cmd = new CreateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    "Tournament Quiz",
                    startTime, endTime, endTime,
                    QuizType.GENERATED, quizExecution, this.quizQuestions);
            this.createdQuizDto = (QuizDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuestionsStep)));

        SagaStep createTournamentStep = new SagaStep("createTournamentStep", () -> {
            CreateTournamentCommand cmd = new CreateTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    this.executionDto.getAggregateId(), this.executionDto.getVersion(),
                    this.executionDto.getCourseId(),
                    this.creatorUserDto.getAggregateId(), this.creatorUserDto.getName(),
                    this.creatorUserDto.getUsername(), this.creatorUserDto.getVersion(),
                    this.topicDtos,
                    this.createdQuizDto.getAggregateId(), this.createdQuizDto.getVersion(),
                    startTime, endTime, numberOfQuestions);
            this.createdTournamentDto = (TournamentDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(createQuizStep)));

        this.workflow.addStep(validateDatesStep);
        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(getStudentStep);
        this.workflow.addStep(getCreatorUserStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(getQuestionsStep);
        this.workflow.addStep(createQuizStep);
        this.workflow.addStep(createTournamentStep);
    }

    public TournamentDto getCreatedTournamentDto() { return createdTournamentDto; }
}
