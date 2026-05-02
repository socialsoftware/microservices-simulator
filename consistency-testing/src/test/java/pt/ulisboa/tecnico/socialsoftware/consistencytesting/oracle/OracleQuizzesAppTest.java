package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.WorkflowUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleQuizzesAppTest {
    private static final String DB_IMAGE = "postgres:15-alpine"; // TODO adjust version as fits best
    private static final String DB_NAME = "oracledb";
    private static final String DB_USERNAME = "oracle";
    private static final String DB_PASSWORD = "postgres"; // TODO is password necessary?

    private final PostgreSQLContainer<?> postgres; // Spins up a throwaway PostgreSQL testcontainer
    private @Nullable Oracle oracle;
    private @Nullable QuizzesTestFactory factory;
    private @Nullable CommandGateway gateway;
    private @Nullable SagaUnitOfWorkService sagaUnitOfWorkService;

    public OracleQuizzesAppTest() {
        postgres = new PostgreSQLContainer<>(DB_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD);
    }

    @BeforeAll
    void setupAll() {
        postgres.start();

        // TODO should this come from maven arguments and profile properites?
        String[] args = {
                "--spring.profiles.active=sagas,local,test",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.datasource.driver-class-name=" + postgres.getDriverClassName(),
                "--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",

                "--spring.jpa.hibernate.ddl-auto=create", // TODO is this needed? was used for testing with jmeter
        };

        oracle = new Oracle(QuizzesSimulator.class, args);
        oracle.init();

        sagaUnitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        gateway = oracle.getBean(CommandGateway.class);

        factory = new QuizzesTestFactory(
                sagaUnitOfWorkService,
                oracle.getBean(ExecutionFunctionalities.class),
                oracle.getBean(UserFunctionalities.class),
                oracle.getBean(TopicFunctionalities.class),
                oracle.getBean(QuestionFunctionalities.class),
                oracle.getBean(TournamentFunctionalities.class),
                oracle.getBean(ImpairmentService.class));
    }

    @AfterAll
    void tearDownAll() {
        oracle.shutdown();
        postgres.stop();
    }

    @Test
    @DisplayName("Single run of 1 functionality should work")
    void singleRunOfFunctionalityShouldWork() {
        Objects.requireNonNull(sagaUnitOfWorkService);

        SagaUnitOfWork uow = sagaUnitOfWorkService.createUnitOfWork(
                AddParticipantFunctionalitySagas.class.getSimpleName());

        Runnable setup = () -> {
            System.out.println("TestCase setup");
        };

        Runnable teardown = () -> {
            System.out.println("TestCase teardown");
        };

        UserDto user = factory.createUser(
                QuizzesTestFactory.USER_NAME_1,
                QuizzesTestFactory.USER_NAME_1,
                QuizzesTestFactory.STUDENT_ROLE);

        CourseExecutionDto courseExecution = factory.createCourseExecution(
                QuizzesTestFactory.COURSE_EXECUTION_NAME,
                QuizzesTestFactory.COURSE_EXECUTION_TYPE,
                QuizzesTestFactory.COURSE_EXECUTION_ACRONYM,
                QuizzesTestFactory.COURSE_EXECUTION_ACADEMIC_TERM,
                QuizzesTestFactory.TIME_4);

        TopicDto topic = factory.createTopic(courseExecution, QuizzesTestFactory.TOPIC_NAME_1);

        QuestionDto question = factory.createQuestion(courseExecution, List.of(topic),
                QuizzesTestFactory.TITLE_1,
                QuizzesTestFactory.CONTENT_1, QuizzesTestFactory.OPTION_1, QuizzesTestFactory.OPTION_2);

        var executionFuncs = oracle.getBean(ExecutionFunctionalities.class);
        executionFuncs.addStudent(courseExecution.getAggregateId(), user.getAggregateId());

        TournamentDto tournament = factory.createTournament(
                QuizzesTestFactory.TIME_1,
                QuizzesTestFactory.TIME_3,
                1,
                user.getAggregateId(),
                courseExecution.getAggregateId(),
                List.of(topic.getAggregateId()));

        var addParticipantSaga = new AddParticipantFunctionalitySagas(
                sagaUnitOfWorkService,
                tournament.getAggregateId(),
                courseExecution.getAggregateId(),
                user.getAggregateId(),
                uow,
                gateway);

        var testCase = new TestCase(List.of(addParticipantSaga), Map.of(), setup, teardown);
        TestResult result = oracle.runTest(testCase);

        List<FlowStep> expectedSchedule = WorkflowUtils.getWorkflowSteps(addParticipantSaga.getWorkflow());
        assertTrue(result.exceptions().isEmpty());
        assertTrue(result.statuses().isEmpty());
        assertEquals(expectedSchedule, result.schedule());
    }
}
