package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.oracle.InitialState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.oracle.QuizzesTestFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;

/** Verifies catalog setup effects never become part of a functionality footprint. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogBaselineQuiescenceTest {

    private static final FunctionalityId SUMMARY = FunctionalityId.forSagaFunctionality("summary");

    @TempDir
    static Path reportsDirectory;

    private TestDriver driver;
    private QuizzesTestFactory factory;
    private TopicFunctionalities topicFunctionalities;
    private SagaUnitOfWorkService unitOfWorkService;
    private CommandGateway commandGateway;

    @BeforeAll
    void startOracle() {
        driver = new TestDriver(QuizzesSimulator.class, List.of(), reportsDirectory);
        driver.init();

        Oracle oracle = driver.getOracle();
        unitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        commandGateway = oracle.getBean(CommandGateway.class);
        topicFunctionalities = oracle.getBean(TopicFunctionalities.class);
        factory = new QuizzesTestFactory(
                unitOfWorkService,
                oracle.getBean(ExecutionFunctionalities.class),
                oracle.getBean(UserFunctionalities.class),
                topicFunctionalities,
                oracle.getBean(QuestionFunctionalities.class),
                oracle.getBean(TournamentFunctionalities.class));
    }

    @AfterAll
    void stopOracle() {
        driver.shutdown();
    }

    @Test
    void profilingExcludesEventHandlerWritesCausedBySetup() {
        FunctionalityCatalog catalog = new FunctionalityCatalog(
                "setup-events-are-baseline",
                () -> {
                    InitialState initialState = factory.setupInitialState();
                    TopicDto changedTopic = new TopicDto();
                    changedTopic.setAggregateId(initialState.topicDto().getAggregateId());
                    changedTopic.setName("Changed during setup");
                    topicFunctionalities.updateTopic(changedTopic);

                    return new AggregateHandlesRegistry()
                            .register("tournament", initialState.tournamentDto().getAggregateId());
                },
                Map.of(SUMMARY, handles -> factory.createGenerateTournamentSummaryFunctionality(
                        unitOfWorkService, handles.idOf("tournament"), commandGateway)));

        FunctionalityFootprint footprint = driver.profileFunctionalities(catalog).get(SUMMARY);

        assertFalse(footprint.writesAnything(),
                "setup's UpdateTopicEvent handlers must drain before summary profiling, accesses="
                        + footprint.accesses());
    }
}
