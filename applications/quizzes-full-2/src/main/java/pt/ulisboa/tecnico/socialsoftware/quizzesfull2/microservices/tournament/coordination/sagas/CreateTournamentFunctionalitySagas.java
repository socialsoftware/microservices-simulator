package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionsByCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.DeleteQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private ExecutionDto executionDto;
    private UserDto creatorDto;
    private QuizDto quizDto;
    private final List<TournamentTopicDto> topics = new ArrayList<>();
    private final List<QuizQuestionDto> selectedQuestions = new ArrayList<>();

    public CreateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer executionAggregateId, Integer creatorAggregateId,
                                              LocalDateTime startTime, LocalDateTime endTime,
                                              Integer numberOfQuestions, List<Integer> topicAggregateIds,
                                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, executionAggregateId, creatorAggregateId, startTime, endTime,
                numberOfQuestions, topicAggregateIds, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId,
                              Integer creatorAggregateId, LocalDateTime startTime, LocalDateTime endTime,
                              Integer numberOfQuestions, List<Integer> topicAggregateIds,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: seeds the TournamentExecution snapshot, including the courseAggregateId
        // that P1 TOPIC_COURSE_EXECUTION reads. The same DTO carries the enrolment list that
        // TournamentService checks for CREATOR_COURSE_EXECUTION (P3).
        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            this.executionDto = (ExecutionDto) commandGateway.send(cmd);
        });

        // P4a prerequisite: seeds the TournamentCreator snapshot. The fetch is the check for
        // CREATOR_EXISTS at create time; CREATOR_IS_NOT_ANONYMOUS is then P1 on the cached fields.
        SagaStep getCreatorStep = new SagaStep("getCreatorStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), creatorAggregateId);
            this.creatorDto = (UserDto) commandGateway.send(cmd);
        });

        // P4a prerequisite: seeds each TournamentTopic snapshot, including its courseAggregateId.
        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            for (Integer topicAggregateId : topicAggregateIds) {
                GetTopicByIdCommand cmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
                TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
                this.topics.add(new TournamentTopicDto(topicDto.getAggregateId(), topicDto.getName(),
                        topicDto.getVersion(), topicDto.getCourseAggregateId()));
            }
        });

        // Assembles the question list the generated quiz will ask. The predicate is the containment
        // NUMBER_OF_QUESTIONS / QUIZ_TOPICS states - a question may only be drawn if the tournament
        // covers every topic it carries - so a question topic outside the tournament's set cannot
        // reach the quiz. The shortfall guard is TournamentService's, not this step's.
        SagaStep selectQuestionsStep = new SagaStep("selectQuestionsStep", () -> {
            GetQuestionsByCourseCommand cmd = new GetQuestionsByCourseCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(),
                    this.executionDto.getCourseAggregateId());
            @SuppressWarnings("unchecked")
            List<QuestionDto> questionsOfCourse = (List<QuestionDto>) commandGateway.send(cmd);
            List<Integer> coveredTopicIds = this.topics.stream()
                    .map(TournamentTopicDto::getTopicAggregateId)
                    .toList();
            questionsOfCourse.stream()
                    .filter(questionDto -> coveredTopicIds.containsAll(questionDto.getTopics().stream()
                            .map(questionTopicDto -> questionTopicDto.getTopicAggregateId())
                            .toList()))
                    .limit(numberOfQuestions)
                    .forEach(questionDto -> this.selectedQuestions.add(new QuizQuestionDto(
                            questionDto.getAggregateId(), questionDto.getVersion(), questionDto.getTitle(),
                            questionDto.getContent())));
        }, new ArrayList<>(Arrays.asList(getExecutionStep, getTopicsStep)));

        // P4b: QUIZ_COURSE_EXECUTION_CONSISTENCY holds because this command and
        // CreateTournamentCommand carry the same execution; START_TIME_AVAILABLE_DATE /
        // END_TIME_CONCLUSION_DATE because availableDate and conclusionDate are the tournament's own
        // startTime and endTime; NUMBER_OF_QUESTIONS / QUIZ_TOPICS because the quiz asks exactly the
        // list selectQuestionsStep drew from the tournament's topics. No enforcement code.
        // The create step takes no semantic lock and declares no forbiddenStates - the quiz does not
        // exist yet - and registers a removal compensation because a later step follows it.
        SagaStep createQuizStep = new SagaStep("createQuizStep", () -> {
            QuizDto newQuizDto = new QuizDto();
            newQuizDto.setExecutionAggregateId(executionAggregateId);
            newQuizDto.setTitle(QuizzesFull2DomainConstants.TOURNAMENT_QUIZ_TITLE);
            newQuizDto.setAvailableDate(startTime);
            newQuizDto.setConclusionDate(endTime);
            newQuizDto.setResultsDate(endTime);
            newQuizDto.setQuizType(QuizType.GENERATED);
            CreateQuizCommand cmd = new CreateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), newQuizDto, this.executionDto,
                    this.selectedQuestions);
            this.quizDto = (QuizDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep, selectQuestionsStep)));

        // Genuine domain-level undo: createTournamentStep runs the CREATOR_COURSE_EXECUTION and
        // TOURNAMENT_NOT_ENOUGH_QUESTIONS guards, and Tournament.quiz being constructor-final forces
        // it to run after the quiz exists. Without this the rejected tournament leaves an orphan quiz.
        createQuizStep.registerCompensation(() -> {
            DeleteQuizCommand cmd = new DeleteQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), this.quizDto.getAggregateId());
            commandGateway.send(cmd);
        }, unitOfWork);

        SagaStep createTournamentStep = new SagaStep("createTournamentStep", () -> {
            CreateTournamentCommand cmd = new CreateTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), this.executionDto,
                    this.creatorDto, this.topics, this.selectedQuestions, this.quizDto.getAggregateId(),
                    this.quizDto.getVersion(), startTime, endTime, numberOfQuestions);
            this.tournamentDto = (TournamentDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getCreatorStep, getTopicsStep, createQuizStep)));

        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(getCreatorStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(selectQuestionsStep);
        this.workflow.addStep(createQuizStep);
        this.workflow.addStep(createTournamentStep);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }
}
