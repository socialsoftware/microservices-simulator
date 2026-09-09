package pt.ulisboa.tecnico.socialsoftware.quizzesfull.oracle;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.functionalities.CourseFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.functionalities.QuizAnswerFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.sagas.*;

@Component
@Profile("oracle")
public class QuizzesFullFunctionalityCatalogsProvider implements FunctionalityCatalogsProvider {

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
            CourseDto course = courseFunctionalities.createCourse("Software Engineering", "TECNICO");
            CourseDto spareCourse = courseFunctionalities.createCourse("Distributed Systems", "TECNICO");

            UserDto student = createUser("John Doe", "johndoe");
            UserDto participant = createUser("Jane Doe", "janedoe");
            UserDto unenrolledStudent = createUser("Alex Doe", "alexdoe");

            ExecutionDto execution = executionFunctionalities.createExecution(
                    "SE01", "1st Semester 2026/2027", course.getAggregateId());
            ExecutionDto emptyExecution = executionFunctionalities.createExecution(
                    "DS01", "1st Semester 2026/2027", course.getAggregateId());
            executionFunctionalities.enrollStudentInExecution(execution.getAggregateId(), student.getAggregateId());
            executionFunctionalities.enrollStudentInExecution(execution.getAggregateId(), participant.getAggregateId());

            TopicDto topic = topicFunctionalities.createTopic(course.getAggregateId(), topic("Architecture"));
            TopicDto secondTopic = topicFunctionalities.createTopic(course.getAggregateId(), topic("Testing"));
            TopicDto standaloneTopic = topicFunctionalities.createTopic(course.getAggregateId(), topic("Databases"));
            QuestionDto question = questionFunctionalities.createQuestion(
                    "Architecture question", "Choose the correct answer", course.getAggregateId(),
                    List.of(topic.getAggregateId()), options());
            QuestionDto secondQuestion = questionFunctionalities.createQuestion(
                    "Testing question", "Choose the correct answer", course.getAggregateId(),
                    List.of(secondTopic.getAggregateId()), options());

            LocalDateTime now = DateHandler.now();
            QuizDto scheduledQuiz = quizFunctionalities.createQuiz(
                    "Scheduled quiz", now.plusDays(1), now.plusDays(2), now.plusDays(3),
                    "GENERATED", execution.getAggregateId(), List.of(question.getAggregateId()));
            QuizAnswerDto quizAnswer = quizAnswerFunctionalities.createQuizAnswer(
                    scheduledQuiz.getAggregateId(), student.getAggregateId());

            TournamentDto openTournament = tournamentFunctionalities.createTournament(
                    execution.getAggregateId(), student.getAggregateId(), List.of(topic.getAggregateId()), 1,
                    now.plusDays(1), now.plusDays(2));
            return new AggregateHandlesRegistry()
                    .register("course", course.getAggregateId())
                    .register("spareCourse", spareCourse.getAggregateId())
                    .register("student", student.getAggregateId())
                    .register("participant", participant.getAggregateId())
                    .register("unenrolledStudent", unenrolledStudent.getAggregateId())
                    .register("execution", execution.getAggregateId())
                    .register("emptyExecution", emptyExecution.getAggregateId())
                    .register("topic", topic.getAggregateId())
                    .register("secondTopic", secondTopic.getAggregateId())
                    .register("standaloneTopic", standaloneTopic.getAggregateId())
                    .register("question", question.getAggregateId())
                    .register("secondQuestion", secondQuestion.getAggregateId())
                    .register("scheduledQuiz", scheduledQuiz.getAggregateId())
                    .register("quizAnswer", quizAnswer.getAggregateId())
                    .register("openTournament", openTournament.getAggregateId())
                    .register("openTournamentQuiz", openTournament.getQuizAggregateId());
        };
    }

    private Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> functionalityFactories() {
        Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories =
                new LinkedHashMap<>();

        add(factories, "createCourse", h -> new CreateCourseFunctionalitySagas(
                unitOfWorkService, "Operating Systems", "TECNICO", uow("createCourse"), commandGateway));
        add(factories, "deleteCourse", h -> new DeleteCourseFunctionalitySagas(
                unitOfWorkService, h.idOf("spareCourse"), uow("deleteCourse"), commandGateway));
        add(factories, "getCourseById", h -> new GetCourseByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("spareCourse"), uow("getCourseById"), commandGateway));
        add(factories, "anonymizeStudent", h -> new AnonymizeStudentFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("participant"),
                uow("anonymizeStudent"), commandGateway));
        add(factories, "createExecution", h -> new CreateExecutionFunctionalitySagas(
                unitOfWorkService, "SE02", "2nd Semester 2026/2027", h.idOf("course"),
                uow("createExecution"), commandGateway));
        add(factories, "deleteExecution", h -> new DeleteExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("emptyExecution"), uow("deleteExecution"), commandGateway));
        add(factories, "disenrollStudent", h -> new DisenrollStudentFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("participant"),
                uow("disenrollStudent"), commandGateway));
        add(factories, "enrollStudentInExecution", h -> new EnrollStudentInExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("unenrolledStudent"),
                uow("enrollStudentInExecution"), commandGateway));
        add(factories, "getExecutionById", h -> new GetExecutionByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), uow("getExecutionById"), commandGateway));
        add(factories, "getStudentByExecutionIdAndUserId", h -> new GetStudentByExecutionIdAndUserIdFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("student"),
                uow("getStudentByExecutionIdAndUserId"), commandGateway));
        add(factories, "updateExecution", h -> new UpdateExecutionFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), "SE-UPDATED", "2027/2028",
                uow("updateExecution"), commandGateway));
        add(factories, "updateStudentName", h -> new UpdateStudentNameFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("participant"), "Jane Updated",
                uow("updateStudentName"), commandGateway));

        add(factories, "createQuestion", h -> new CreateQuestionFunctionalitySagas(
                unitOfWorkService, "New question", "New content", h.idOf("course"), List.of(h.idOf("topic")),
                options(), uow("createQuestion"), commandGateway));
        add(factories, "deleteQuestion", h -> new DeleteQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("secondQuestion"), uow("deleteQuestion"), commandGateway));
        add(factories, "getQuestionById", h -> new GetQuestionByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("question"), uow("getQuestionById"), commandGateway));
        add(factories, "getQuestionsByCourseExecutionId", h -> new GetQuestionsByCourseExecutionIdFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), uow("getQuestionsByCourseExecutionId"), commandGateway));
        add(factories, "updateQuestion", h -> new UpdateQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("secondQuestion"), "Updated question", "Updated content",
                List.of(h.idOf("secondTopic")), uow("updateQuestion"), commandGateway));

        add(factories, "createQuiz", h -> new CreateQuizFunctionalitySagas(
                unitOfWorkService, "New quiz", DateHandler.now().plusDays(1), DateHandler.now().plusDays(2),
                DateHandler.now().plusDays(3), "GENERATED", h.idOf("execution"), List.of(h.idOf("secondQuestion")),
                uow("createQuiz"), commandGateway));
        add(factories, "getQuizById", h -> new GetQuizByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("scheduledQuiz"), uow("getQuizById"), commandGateway));
        add(factories, "updateQuiz", h -> new UpdateQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("scheduledQuiz"), DateHandler.now().plusDays(2),
                DateHandler.now().plusDays(4), DateHandler.now().plusDays(5), List.of(h.idOf("secondQuestion")),
                uow("updateQuiz"), commandGateway));

        add(factories, "answerQuestion", h -> new AnswerQuestionFunctionalitySagas(
                unitOfWorkService, h.idOf("quizAnswer"), h.idOf("question"), 1, 30,
                uow("answerQuestion"), commandGateway));
        add(factories, "concludeQuiz", h -> new ConcludeQuizFunctionalitySagas(
                unitOfWorkService, h.idOf("quizAnswer"), uow("concludeQuiz"), commandGateway));
        add(factories, "createQuizAnswer", h -> new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, h.idOf("scheduledQuiz"), h.idOf("participant"),
                uow("createQuizAnswer"), commandGateway));
        add(factories, "getQuizAnswerByQuizIdAndStudentId", h ->
                new GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas(
                        unitOfWorkService, h.idOf("scheduledQuiz"), h.idOf("student"),
                        uow("getQuizAnswerByQuizIdAndStudentId"), commandGateway));

        add(factories, "createTopic", h -> new CreateTopicFunctionalitySagas(
                unitOfWorkService, h.idOf("course"), topic("New topic"), uow("createTopic"), commandGateway));
        add(factories, "deleteTopic", h -> new DeleteTopicFunctionalitySagas(
                unitOfWorkService, h.idOf("standaloneTopic"), uow("deleteTopic"), commandGateway));
        add(factories, "getTopicById", h -> new GetTopicByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("topic"), uow("getTopicById"), commandGateway));
        add(factories, "getTopicsByCourseId", h -> new GetTopicsByCourseIdFunctionalitySagas(
                unitOfWorkService, h.idOf("course"), uow("getTopicsByCourseId"), commandGateway));
        add(factories, "updateTopic", h -> {
            TopicDto dto = topic("Updated topic");
            dto.setAggregateId(h.idOf("topic"));
            return new UpdateTopicFunctionalitySagas(unitOfWorkService, dto, uow("updateTopic"), commandGateway);
        });

        add(factories, "addParticipant", h -> new AddParticipantFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), h.idOf("execution"), h.idOf("participant"),
                uow("addParticipant"), commandGateway));
        add(factories, "cancelTournament", h -> new CancelTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), uow("cancelTournament"), commandGateway));
        add(factories, "createTournament", h -> new CreateTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), h.idOf("student"), List.of(h.idOf("secondTopic")), 1,
                DateHandler.now().plusDays(3), DateHandler.now().plusDays(4),
                uow("createTournament"), commandGateway));
        add(factories, "deleteTournament", h -> new DeleteTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), uow("deleteTournament"), commandGateway));
        add(factories, "getOpenTournaments", h -> new GetOpenTournamentsFunctionalitySagas(
                unitOfWorkService, h.idOf("execution"), uow("getOpenTournaments"), commandGateway));
        add(factories, "getTournamentById", h -> new GetTournamentByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), uow("getTournamentById"), commandGateway));
        add(factories, "updateTournament", h -> new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, h.idOf("openTournament"), DateHandler.now().plusDays(2),
                DateHandler.now().plusDays(3), List.of(h.idOf("topic")),
                uow("updateTournament"), commandGateway));

        add(factories, "anonymizeUser", h -> new AnonymizeUserFunctionalitySagas(
                unitOfWorkService, h.idOf("unenrolledStudent"), uow("anonymizeUser"), commandGateway));
        add(factories, "createUser", h -> new CreateUserFunctionalitySagas(
                unitOfWorkService, new UserDto(null, "New User", "newuser", "STUDENT", false),
                uow("createUser"), commandGateway));
        add(factories, "deleteUser", h -> new DeleteUserFunctionalitySagas(
                unitOfWorkService, h.idOf("unenrolledStudent"), uow("deleteUser"), commandGateway));
        add(factories, "getUserById", h -> new GetUserByIdFunctionalitySagas(
                unitOfWorkService, h.idOf("student"), uow("getUserById"), commandGateway));
        add(factories, "updateUserName", h -> new UpdateUserNameFunctionalitySagas(
                unitOfWorkService, h.idOf("unenrolledStudent"), "Alex Updated", uow("updateUserName"), commandGateway));

        return factories;
    }

    private void add(Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories,
                     String id, Function<AggregateHandlesRegistry, WorkflowFunctionality> factory) {
        factories.put(FunctionalityId.forSagaFunctionality(id), factory);
    }

    private SagaUnitOfWork uow(String functionalityName) {
        return unitOfWorkService.createUnitOfWork(functionalityName);
    }

    private UserDto createUser(String name, String username) {
        return userFunctionalities.createUser(new UserDto(null, name, username, "STUDENT", false));
    }

    private TopicDto topic(String name) {
        TopicDto dto = new TopicDto();
        dto.setName(name);
        return dto;
    }

    private Set<Option> options() {
        return Set.of(new Option(1, 1, "Option A", true), new Option(2, 2, "Option B", false));
    }

}
