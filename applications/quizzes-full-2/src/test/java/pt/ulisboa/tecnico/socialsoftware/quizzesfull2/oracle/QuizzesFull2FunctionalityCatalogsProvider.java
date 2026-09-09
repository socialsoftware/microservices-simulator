package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.oracle;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
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
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.functionalities.CourseFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.functionalities.QuizAnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.sagas.*;

@Component
@Profile("oracle")
public class QuizzesFull2FunctionalityCatalogsProvider implements FunctionalityCatalogsProvider {

    private static final String CATALOG = "ordinary-shared-state";

    @Autowired private SagaUnitOfWorkService unitOfWorkService;
    @Autowired private CommandGateway commandGateway;
    @Autowired private CourseFunctionalities courseFunctionalities;
    @Autowired private ExecutionFunctionalities executionFunctionalities;
    @Autowired private QuestionFunctionalities questionFunctionalities;
    @Autowired private QuizFunctionalities quizFunctionalities;
    @Autowired private QuizAnswerFunctionalities quizAnswerFunctionalities;
    @Autowired private TopicFunctionalities topicFunctionalities;
    @Autowired private TournamentFunctionalities tournamentFunctionalities;
    @Autowired private UserFunctionalities userFunctionalities;

    @Override
    public List<FunctionalityCatalog> getCatalogs() {
        return List.of(new FunctionalityCatalog(CATALOG, initialStateSetup(), functionalityFactories()));
    }

    private Supplier<AggregateHandlesRegistry> initialStateSetup() {
        return () -> {
            CourseDto course = courseFunctionalities.createCourse(course("Software Engineering"));

            UserDto student = createUser("John Doe", "johndoe", Role.STUDENT, true);
            UserDto participant = createUser("Jane Doe", "janedoe", Role.STUDENT, true);
            UserDto unenrolledStudent = createUser("Alex Doe", "alexdoe", Role.STUDENT, true);
            UserDto inactiveStudent = createUser("Inactive Doe", "inactivedoe", Role.STUDENT, false);
            UserDto disposableUser = createUser("Delete Doe", "deletedoe", Role.STUDENT, false);
            UserDto teacher = createUser("Teacher Doe", "teacherdoe", Role.TEACHER, false);

            ExecutionDto execution = executionFunctionalities.createExecution(
                    execution("SE01", "1st Semester 2026/2027", course.getAggregateId()));
            ExecutionDto emptyExecution = executionFunctionalities.createExecution(
                    execution("SE02", "2nd Semester 2026/2027", course.getAggregateId()));
            ExecutionDto disenrollmentExecution = executionFunctionalities.createExecution(
                    execution("SE04", "Winter 2027", course.getAggregateId()));
            executionFunctionalities.enrollStudentInExecution(execution.getAggregateId(), student.getAggregateId());
            executionFunctionalities.enrollStudentInExecution(
                    execution.getAggregateId(), participant.getAggregateId());
            executionFunctionalities.enrollStudentInExecution(
                    disenrollmentExecution.getAggregateId(), participant.getAggregateId());

            TopicDto topic = topicFunctionalities.createTopic(topic("Architecture", course.getAggregateId()));
            TopicDto secondTopic = topicFunctionalities.createTopic(topic("Testing", course.getAggregateId()));
            TopicDto standaloneTopic = topicFunctionalities.createTopic(topic("Databases", course.getAggregateId()));

            QuestionDto question = questionFunctionalities.createQuestion(
                    question("Architecture question", course.getAggregateId()), List.of(topic.getAggregateId()));
            QuestionDto secondQuestion = questionFunctionalities.createQuestion(
                    question("Testing question", course.getAggregateId()), List.of(secondTopic.getAggregateId()));
            QuestionDto disposableQuestion = questionFunctionalities.createQuestion(
                    question("Database question", course.getAggregateId()), List.of(standaloneTopic.getAggregateId()));

            LocalDateTime now = DateHandler.now();
            QuizDto scheduledQuiz = quizFunctionalities.createQuiz(
                    quiz("Scheduled quiz", execution.getAggregateId(), now.plusDays(2), now.plusDays(3),
                            now.plusDays(4)),
                    List.of(question.getAggregateId()));
            QuizAnswerDto quizAnswer = quizAnswerFunctionalities.createQuizAnswer(
                    scheduledQuiz.getAggregateId(), student.getAggregateId(), execution.getAggregateId());

            TournamentDto openTournament = tournamentFunctionalities.createTournament(
                    execution.getAggregateId(), student.getAggregateId(), now.plusDays(5), now.plusDays(6), 1,
                    List.of(topic.getAggregateId()));
            TournamentDto cancelledTournament = tournamentFunctionalities.createTournament(
                    execution.getAggregateId(), student.getAggregateId(), now.plusDays(7), now.plusDays(8), 1,
                    List.of(secondTopic.getAggregateId()));
            tournamentFunctionalities.cancelTournament(cancelledTournament.getAggregateId());

            return new AggregateHandlesRegistry()
                    .register("course", course.getAggregateId())
                    .register("student", student.getAggregateId())
                    .register("participant", participant.getAggregateId())
                    .register("unenrolledStudent", unenrolledStudent.getAggregateId())
                    .register("inactiveStudent", inactiveStudent.getAggregateId())
                    .register("disposableUser", disposableUser.getAggregateId())
                    .register("teacher", teacher.getAggregateId())
                    .register("execution", execution.getAggregateId())
                    .register("emptyExecution", emptyExecution.getAggregateId())
                    .register("disenrollmentExecution", disenrollmentExecution.getAggregateId())
                    .register("topic", topic.getAggregateId())
                    .register("secondTopic", secondTopic.getAggregateId())
                    .register("standaloneTopic", standaloneTopic.getAggregateId())
                    .register("question", question.getAggregateId())
                    .register("secondQuestion", secondQuestion.getAggregateId())
                    .register("disposableQuestion", disposableQuestion.getAggregateId())
                    .register("scheduledQuiz", scheduledQuiz.getAggregateId())
                    .register("quizAnswer", quizAnswer.getAggregateId())
                    .register("openTournament", openTournament.getAggregateId())
                    .register("openTournamentQuiz", openTournament.getQuizAggregateId())
                    .register("cancelledTournament", cancelledTournament.getAggregateId())
                    .register("cancelledTournamentQuiz", cancelledTournament.getQuizAggregateId());
        };
    }

    private Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> functionalityFactories() {
        Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories =
                new LinkedHashMap<>();

        add(factories, "createCourse", h -> new CreateCourseFunctionalitySagas(
                unitOfWorkService, course("Distributed Systems"), uow("createCourse"), commandGateway));
        add(factories, "getCourseById", h -> new GetCourseByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("course"), uow("getCourseById"), commandGateway));
        add(factories, "getCourses", h -> new GetCoursesFunctionalitySagas(
                unitOfWorkService, uow("getCourses"), commandGateway));

        add(factories, "createExecution", h -> new CreateExecutionFunctionalitySagas(
                unitOfWorkService, execution("SE03", "Summer 2027", h.idOf("course")),
                uow("createExecution"), commandGateway));
        add(factories, "deleteExecution", h -> new DeleteExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("emptyExecution"), uow("deleteExecution"), commandGateway));
        add(factories, "disenrollStudent", h -> new DisenrollStudentFunctionalitySagas(
                unitOfWorkService, h.idOf("disenrollmentExecution"), h.idOf("participant"),
                uow("disenrollStudent"), commandGateway));
        add(factories, "enrollStudentInExecution", h -> new EnrollStudentInExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("unenrolledStudent"),
                uow("enrollStudentInExecution"), commandGateway));
        add(factories, "getExecutionById", h -> new GetExecutionByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), uow("getExecutionById"), commandGateway));
        add(factories, "getExecutions", h -> new GetExecutionsFunctionalitySagas(
                unitOfWorkService, uow("getExecutions"), commandGateway));
        add(factories, "getUserExecutions", h -> new GetUserExecutionsFunctionalitySagas(
                unitOfWorkService, h.idOf("student"), uow("getUserExecutions"), commandGateway));
        add(factories, "updateExecution", h -> new UpdateExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), "SE-UPDATED", "2027/2028",
                uow("updateExecution"), commandGateway));

        add(factories, "createQuestion", h -> new CreateQuestionFunctionalitySagas(
                unitOfWorkService, question("New question", h.idOf("course")), List.of(h.idOf("topic")),
                uow("createQuestion"), commandGateway));
        add(factories, "deleteQuestion", h -> new DeleteQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("disposableQuestion"), uow("deleteQuestion"), commandGateway));
        add(factories, "getQuestionById", h -> new GetQuestionByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("question"), uow("getQuestionById"), commandGateway));
        add(factories, "getQuestionsByCourse", h -> new GetQuestionsByCourseFunctionalitySagas(
                unitOfWorkService, h.idOf("course"), uow("getQuestionsByCourse"), commandGateway));
        add(factories, "updateQuestion", h -> new UpdateQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("secondQuestion"), "Updated question", "Updated content",
                List.of(h.idOf("secondTopic")), uow("updateQuestion"), commandGateway));

        add(factories, "createQuiz", h -> new CreateQuizFunctionalitySagas(
                unitOfWorkService, quiz("New quiz", h.idOf("execution"), DateHandler.now().plusDays(2),
                        DateHandler.now().plusDays(3), DateHandler.now().plusDays(4)),
                List.of(h.idOf("secondQuestion")), uow("createQuiz"), commandGateway));
        add(factories, "getQuizById", h -> new GetQuizByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("scheduledQuiz"), uow("getQuizById"), commandGateway));
        add(factories, "getQuizzesForExecution", h -> new GetQuizzesForExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), uow("getQuizzesForExecution"), commandGateway));
        add(factories, "updateQuiz", h -> new UpdateQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("scheduledQuiz"), "Updated quiz", DateHandler.now().plusDays(3),
                DateHandler.now().plusDays(4), DateHandler.now().plusDays(5),
                List.of(h.idOf("secondQuestion")), uow("updateQuiz"), commandGateway));

        add(factories, "answerQuestion", h -> new AnswerQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("quizAnswer"), h.idOf("question"), 1, 1, 30,
                uow("answerQuestion"), commandGateway));
        add(factories, "concludeQuiz", h -> new ConcludeQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("quizAnswer"), uow("concludeQuiz"), commandGateway));
        add(factories, "createQuizAnswer", h -> new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, h.idOf("scheduledQuiz"), h.idOf("participant"), h.idOf("execution"),
                uow("createQuizAnswer"), commandGateway));
        add(factories, "getQuizAnswerById", h -> new GetQuizAnswerByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("quizAnswer"), uow("getQuizAnswerById"), commandGateway));
        add(factories, "getQuizAnswerForStudentAndQuiz", h ->
                new GetQuizAnswerForStudentAndQuizFunctionalitySagas(
                        unitOfWorkService, h.idOf("student"), h.idOf("scheduledQuiz"),
                        uow("getQuizAnswerForStudentAndQuiz"), commandGateway));

        add(factories, "createTopic", h -> new CreateTopicFunctionalitySagas(
                unitOfWorkService, topic("New topic", h.idOf("course")), uow("createTopic"), commandGateway));
        add(factories, "deleteTopic", h -> new DeleteTopicFunctionalitySagas(
                unitOfWorkService, h.idOf("standaloneTopic"), uow("deleteTopic"), commandGateway));
        add(factories, "getTopicsByCourse", h -> new GetTopicsByCourseFunctionalitySagas(
                unitOfWorkService, h.idOf("course"), uow("getTopicsByCourse"), commandGateway));
        add(factories, "updateTopic", h -> new UpdateTopicFunctionalitySagas(
                unitOfWorkService, h.idOf("topic"), "Updated topic", uow("updateTopic"), commandGateway));

        add(factories, "addParticipant", h -> new AddParticipantFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), h.idOf("participant"),
                uow("addParticipant"), commandGateway));
        add(factories, "cancelTournament", h -> new CancelTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), uow("cancelTournament"), commandGateway));
        add(factories, "createTournament", h -> new CreateTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("student"), DateHandler.now().plusDays(9),
                DateHandler.now().plusDays(10), 1, List.of(h.idOf("secondTopic")),
                uow("createTournament"), commandGateway));
        add(factories, "deleteTournament", h -> new DeleteTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("cancelledTournament"), uow("deleteTournament"), commandGateway));
        add(factories, "getClosedTournamentsForExecution", h ->
                new GetClosedTournamentsForExecutionFunctionalitySagas(
                        unitOfWorkService, h.idOf("execution"),
                        uow("getClosedTournamentsForExecution"), commandGateway));
        add(factories, "getOpenedTournamentsForExecution", h ->
                new GetOpenedTournamentsForExecutionFunctionalitySagas(
                        unitOfWorkService, h.idOf("execution"),
                        uow("getOpenedTournamentsForExecution"), commandGateway));
        add(factories, "getTournamentById", h -> new GetTournamentByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), uow("getTournamentById"), commandGateway));
        add(factories, "getTournamentsForExecution", h -> new GetTournamentsForExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), uow("getTournamentsForExecution"), commandGateway));
        add(factories, "updateTournament", h -> new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), DateHandler.now().plusDays(6),
                DateHandler.now().plusDays(7), 1, List.of(h.idOf("topic")),
                uow("updateTournament"), commandGateway));

        add(factories, "activateUser", h -> new ActivateUserFunctionalitySagas(
                unitOfWorkService, h.idOf("inactiveStudent"), uow("activateUser"), commandGateway));
        add(factories, "anonymizeUser", h -> new AnonymizeUserFunctionalitySagas(
                unitOfWorkService, h.idOf("disposableUser"), uow("anonymizeUser"), commandGateway));
        add(factories, "createUser", h -> new CreateUserFunctionalitySagas(
                unitOfWorkService, user("New User", "newuser", Role.STUDENT), uow("createUser"), commandGateway));
        add(factories, "deleteUser", h -> new DeleteUserFunctionalitySagas(
                unitOfWorkService, h.idOf("disposableUser"), uow("deleteUser"), commandGateway));
        add(factories, "getStudents", h -> new GetStudentsFunctionalitySagas(
                unitOfWorkService, uow("getStudents"), commandGateway));
        add(factories, "getTeachers", h -> new GetTeachersFunctionalitySagas(
                unitOfWorkService, uow("getTeachers"), commandGateway));
        add(factories, "getUserById", h -> new GetUserByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("student"), uow("getUserById"), commandGateway));
        add(factories, "updateUserName", h -> new UpdateUserNameFunctionalitySagas(
                unitOfWorkService, h.idOf("disposableUser"), "Updated User",
                uow("updateUserName"), commandGateway));

        return factories;
    }

    private void add(Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories,
                     String id, Function<AggregateHandlesRegistry, WorkflowFunctionality> factory) {
        factories.put(FunctionalityId.forSagaFunctionality(id), factory);
    }

    private SagaUnitOfWork uow(String functionalityName) {
        return unitOfWorkService.createUnitOfWork(functionalityName);
    }

    private UserDto createUser(String name, String username, Role role, boolean active) {
        UserDto created = userFunctionalities.createUser(user(name, username, role));
        if (active) {
            userFunctionalities.activateUser(created.getAggregateId());
            return userFunctionalities.getUserById(created.getAggregateId());
        }
        return created;
    }

    private CourseDto course(String name) {
        return new CourseDto(null, null, name, CourseType.TECNICO);
    }

    private UserDto user(String name, String username, Role role) {
        return new UserDto(null, null, name, username, role, false);
    }

    private ExecutionDto execution(String acronym, String academicTerm, Integer courseAggregateId) {
        return new ExecutionDto(null, null, acronym, academicTerm, DateHandler.now().plusYears(1),
                courseAggregateId, null, null, List.of());
    }

    private TopicDto topic(String name, Integer courseAggregateId) {
        return new TopicDto(null, null, name, courseAggregateId);
    }

    private QuestionDto question(String title, Integer courseAggregateId) {
        return new QuestionDto(null, null, title, "Choose the correct answer", DateHandler.now(),
                courseAggregateId,
                List.of(new OptionDto(1, 1, "Option A", true),
                        new OptionDto(2, 2, "Option B", false)),
                List.of());
    }

    private QuizDto quiz(String title, Integer executionAggregateId, LocalDateTime availableDate,
                         LocalDateTime conclusionDate, LocalDateTime resultsDate) {
        return new QuizDto(null, null, title, null, availableDate, conclusionDate, resultsDate,
                QuizType.GENERATED, executionAggregateId, null, List.of());
    }
}
