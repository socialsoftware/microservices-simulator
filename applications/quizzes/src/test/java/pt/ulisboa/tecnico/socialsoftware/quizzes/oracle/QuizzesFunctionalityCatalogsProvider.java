package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.FunctionalityCatalogsProvider;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.AggregateHandlesRegistry;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

@Component
@Profile("oracle")
public class QuizzesFunctionalityCatalogsProvider implements FunctionalityCatalogsProvider {

    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private final UserService userService;
    private final ExecutionFunctionalities executions;
    private final UserFunctionalities users;
    private final TopicFunctionalities topics;
    private final QuestionFunctionalities questions;
    private final QuizFunctionalities quizzes;
    private final QuizAnswerFunctionalities answers;
    private final TournamentFunctionalities tournaments;
    private final QuizAnswerRepository quizAnswerRepository;

    public QuizzesFunctionalityCatalogsProvider(
            SagaUnitOfWorkService unitOfWorkService,
            CommandGateway commandGateway,
            UserService userService,
            ExecutionFunctionalities executions,
            UserFunctionalities users,
            TopicFunctionalities topics,
            QuestionFunctionalities questions,
            QuizFunctionalities quizzes,
            QuizAnswerFunctionalities answers,
            TournamentFunctionalities tournaments,
            QuizAnswerRepository quizAnswerRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.userService = userService;
        this.executions = executions;
        this.users = users;
        this.topics = topics;
        this.questions = questions;
        this.quizzes = quizzes;
        this.answers = answers;
        this.tournaments = tournaments;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    @Override
    public List<FunctionalityCatalog> getCatalogs() {
        return List.of(new FunctionalityCatalog("neutral-shared-state", this::setupState, factories()));
    }

    private AggregateHandlesRegistry setupState() {
        return new QuizzesConsistencyStateFactory(
                executions, users, topics, questions, quizzes, answers, tournaments, quizAnswerRepository).setup();
    }

    private Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories() {
        Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories =
                new LinkedHashMap<>();

        add(factories, "startQuiz", h -> new StartQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("quiz"), h.idOf("mainExecution"), h.idOf("startStudent"),
                uow("startQuiz"), commandGateway));

        add(factories, "addStudent", h -> new AddStudentFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), h.idOf("addStudent"), uow("addStudent"), commandGateway));
        add(factories, "anonymizeStudent", h -> new AnonymizeStudentFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), h.idOf("participant"), uow("anonymizeStudent"), commandGateway));
        add(factories, "createCourseExecution", h -> new CreateCourseExecutionFunctionalitySagas(
                unitOfWorkService, executionInput(), uow("createCourseExecution"), commandGateway));
        add(factories, "getCourseExecutionById", h -> new GetCourseExecutionByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), uow("getCourseExecutionById"), commandGateway));
        add(factories, "getCourseExecutionsByUser", h -> new GetCourseExecutionsByUserFunctionalitySagas(
                unitOfWorkService, h.idOf("participant"), uow("getCourseExecutionsByUser"), commandGateway));
        add(factories, "getCourseExecutions", h -> new GetCourseExecutionsFunctionalitySagas(
                unitOfWorkService, uow("getCourseExecutions"), commandGateway));
        add(factories, "removeCourseExecution", h -> new pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveCourseExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("emptyExecution"), uow("removeCourseExecution"), commandGateway));
        add(factories, "removeStudent", h -> new RemoveStudentFromCourseExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), h.idOf("startStudent"), uow("removeStudent"), commandGateway));
        add(factories, "updateStudentName", h -> new UpdateStudentNameFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), h.idOf("participant"), userNameInput("Updated Participant"),
                uow("updateStudentName"), commandGateway));

        add(factories, "createQuestion", h -> new CreateQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("mainCourse"), questionInput(h, "Created question"),
                uow("createQuestion"), commandGateway));
        add(factories, "findQuestionById", h -> new FindQuestionByAggregateIdFunctionalitySagas(
                unitOfWorkService, h.idOf("primaryQuestion"), uow("findQuestionById"), commandGateway));
        add(factories, "findQuestionsByCourse", h -> new FindQuestionsByCourseFunctionalitySagas(
                unitOfWorkService, h.idOf("mainCourse"), uow("findQuestionsByCourse"), commandGateway));
        add(factories, "removeQuestion", h -> new RemoveQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("disposableQuestion"), uow("removeQuestion"), commandGateway));
        add(factories, "updateQuestion", h -> new UpdateQuestionFunctionalitySagas(
                unitOfWorkService, questionUpdate(h.idOf("primaryQuestion")), uow("updateQuestion"), commandGateway));
        add(factories, "updateQuestionTopics", h -> new UpdateQuestionTopicsFunctionalitySagas(
                unitOfWorkService, h.idOf("primaryQuestion"), List.of(h.idOf("secondaryTopic")),
                uow("updateQuestionTopics"), commandGateway));
        add(factories, "updateQuestionTopicsAsync", h -> new UpdateQuestionTopicsAsyncFunctionalitySagas(
                unitOfWorkService, h.idOf("primaryQuestion"), List.of(h.idOf("secondaryTopic")),
                uow("updateQuestionTopicsAsync"), commandGateway));

        add(factories, "createQuiz", h -> new CreateQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), quizInput(h, "Created quiz"),
                uow("createQuiz"), commandGateway));
        add(factories, "findQuiz", h -> new FindQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("quiz"), uow("findQuiz"), commandGateway));
        add(factories, "getAvailableQuizzes", h -> new GetAvailableQuizzesFunctionalitySagas(
                unitOfWorkService, h.idOf("participant"), h.idOf("mainExecution"),
                uow("getAvailableQuizzes"), commandGateway));
        add(factories, "updateQuiz", h -> new UpdateQuizFunctionalitySagas(
                unitOfWorkService, quizUpdate(h), uow("updateQuiz"), commandGateway));

        add(factories, "createTopic", h -> new CreateTopicFunctionalitySagas(
                unitOfWorkService, h.idOf("mainCourse"), topicInput(null, "Created topic"),
                uow("createTopic"), commandGateway));
        add(factories, "deleteTopic", h -> new pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.DeleteTopicFunctionalitySagas(
                unitOfWorkService, h.idOf("disposableTopic"), uow("deleteTopic"), commandGateway));
        add(factories, "findTopicsByCourse", h -> new FindTopicsByCourseFunctionalitySagas(
                unitOfWorkService, h.idOf("mainCourse"), uow("findTopicsByCourse"), commandGateway));
        add(factories, "getTopicById", h -> new GetTopicByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("primaryTopic"), uow("getTopicById"), commandGateway));
        add(factories, "updateTopic", h -> new pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas(
                unitOfWorkService, topicInput(h.idOf("primaryTopic"), "Updated topic"),
                uow("updateTopic"), commandGateway));

        add(factories, "addParticipant", h -> new AddParticipantFunctionalitySagas(
                unitOfWorkService, h.idOf("cancellableTournament"), h.idOf("mainExecution"), h.idOf("startStudent"),
                uow("addParticipant"), commandGateway));
        add(factories, "addParticipantAsync", h -> new AddParticipantAsyncFunctionalitySagas(
                unitOfWorkService, h.idOf("cancellableTournament"), h.idOf("mainExecution"), h.idOf("startStudent"),
                uow("addParticipantAsync"), commandGateway));
        add(factories, "cancelTournament", h -> new CancelTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("cancellableTournament"), uow("cancelTournament"), commandGateway));
        add(factories, "createTournament", h -> new CreateTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("creator"), h.idOf("mainExecution"), List.of(h.idOf("primaryTopic")),
                tournamentInput(), uow("createTournament"), commandGateway));
        add(factories, "createTournamentAsync", h -> new CreateTournamentAsyncFunctionalitySagas(
                unitOfWorkService, h.idOf("creator"), h.idOf("mainExecution"), List.of(h.idOf("primaryTopic")),
                tournamentInput(), uow("createTournamentAsync"), commandGateway));
        add(factories, "findParticipant", h -> new FindParticipantFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), h.idOf("participant"),
                uow("findParticipant"), commandGateway));
        add(factories, "findTournament", h -> new FindTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), uow("findTournament"), commandGateway));
        add(factories, "getClosedTournaments", h -> new GetClosedTournamentsForCourseExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), uow("getClosedTournaments"), commandGateway));
        add(factories, "getOpenedTournaments", h -> new GetOpenedTournamentsForCourseExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), uow("getOpenedTournaments"), commandGateway));
        add(factories, "getTournaments", h -> new GetTournamentsForCourseExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("mainExecution"), uow("getTournaments"), commandGateway));
        add(factories, "leaveTournament", h -> new LeaveTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), h.idOf("participant"),
                uow("leaveTournament"), commandGateway));
        add(factories, "removeTournament", h -> new pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("removableTournament"), uow("removeTournament"), commandGateway));
        add(factories, "solveQuiz", h -> new SolveQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), h.idOf("participant"), uow("solveQuiz"), commandGateway));
        add(factories, "solveQuizAsync", h -> new SolveQuizAsyncFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), h.idOf("participant"),
                uow("solveQuizAsync"), commandGateway));
        add(factories, "updateTournament", h -> new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, tournamentUpdate(h.idOf("openTournament")), Set.of(h.idOf("primaryTopic")),
                uow("updateTournament"), commandGateway));

        add(factories, "activateUser", h -> new ActivateUserFunctionalitySagas(
                unitOfWorkService, h.idOf("inactiveUser"), uow("activateUser"), commandGateway));
        add(factories, "createUser", h -> new CreateUserFunctionalitySagas(
                unitOfWorkService, userInput("Created User", "created-user"), uow("createUser"), commandGateway));
        add(factories, "deactivateUser", h -> new DeactivateUserFunctionalitySagas(
                unitOfWorkService, h.idOf("deleteUser"), uow("deactivateUser"), commandGateway));
        add(factories, "deleteUser", h -> new DeleteUserFunctionalitySagas(
                userService, unitOfWorkService, h.idOf("deleteUser"), uow("deleteUser"), commandGateway));
        add(factories, "findUserById", h -> new FindUserByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("participant"), uow("findUserById"), commandGateway));
        add(factories, "getStudents", h -> new GetStudentsFunctionalitySagas(
                unitOfWorkService, uow("getStudents"), commandGateway));
        add(factories, "getTeachers", h -> new GetTeachersFunctionalitySagas(
                unitOfWorkService, uow("getTeachers"), commandGateway));

        return factories;
    }

    private void add(
            Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories,
            String id,
            Function<AggregateHandlesRegistry, WorkflowFunctionality> factory) {
        factories.put(FunctionalityId.forSagaFunctionality(id), factory);
    }

    private SagaUnitOfWork uow(String name) {
        return unitOfWorkService.createUnitOfWork(name);
    }

    private static CourseExecutionDto executionInput() {
        CourseExecutionDto dto = new CourseExecutionDto();
        dto.setName("Created Course");
        dto.setType("TECNICO");
        dto.setAcronym("CREATED");
        dto.setAcademicTerm("2027");
        dto.setEndDate(pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.toISOString(
                pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.now().plusDays(3)));
        return dto;
    }

    private static QuestionDto questionInput(AggregateHandlesRegistry handles, String title) {
        TopicDto topic = topicInput(handles.idOf("primaryTopic"), "Primary Topic");
        return QuizzesConsistencyStateFactory.questionInput(title, List.of(topic));
    }

    private static QuestionDto questionUpdate(Integer aggregateId) {
        QuestionDto dto = new QuestionDto();
        dto.setAggregateId(aggregateId);
        dto.setTitle("Updated question");
        dto.setContent("Updated content");
        return dto;
    }

    private static QuizDto quizInput(AggregateHandlesRegistry handles, String title) {
        QuestionDto question = new QuestionDto();
        question.setAggregateId(handles.idOf("primaryQuestion"));
        return QuizzesConsistencyStateFactory.quizInput(title, List.of(question));
    }

    private static QuizDto quizUpdate(AggregateHandlesRegistry handles) {
        QuizDto dto = new QuizDto();
        dto.setAggregateId(handles.idOf("quiz"));
        dto.setTitle("Updated quiz");
        QuestionDto question = new QuestionDto();
        question.setAggregateId(handles.idOf("primaryQuestion"));
        dto.setQuestionDtos(List.of(question));
        return dto;
    }

    private static TopicDto topicInput(Integer aggregateId, String name) {
        TopicDto dto = new TopicDto();
        dto.setAggregateId(aggregateId);
        dto.setName(name);
        return dto;
    }

    private static TournamentDto tournamentInput() {
        TournamentDto dto = new TournamentDto();
        dto.setStartTime(pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.toISOString(
                pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.now().plusMinutes(15)));
        dto.setEndTime(pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.toISOString(
                pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler.now().plusHours(2)));
        dto.setNumberOfQuestions(1);
        return dto;
    }

    private static TournamentDto tournamentUpdate(Integer aggregateId) {
        TournamentDto dto = tournamentInput();
        dto.setAggregateId(aggregateId);
        return dto;
    }

    private static UserDto userInput(String name, String username) {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setUsername(username);
        dto.setRole("STUDENT");
        return dto;
    }

    private static UserDto userNameInput(String name) {
        UserDto dto = new UserDto();
        dto.setName(name);
        return dto;
    }

}
