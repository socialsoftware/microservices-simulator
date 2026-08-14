package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.AnomalyType;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testsupport.InitialState;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testsupport.QuizzesTestFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

/**
 * End-to-end validation of the {@link FunctionalityGroupPlanner} pipeline on
 * the quizzes app: profile a "chaotic" pool of real functionalities running
 * over a shared initial state, plan the conflict groups, and then let the
 * {@link TestDriver} explore the planner's own choices to confirm they retain
 * the detection power of the hand-picked pairs in
 * {@link TestDriverQuizzesAppTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FunctionalityGroupPlannerQuizzesAppTest {

    private static final Logger log = LoggerFactory.getLogger(FunctionalityGroupPlannerQuizzesAppTest.class);

    private static final int ITERATIONS = 20;

    // The catalog pool. Names say what each instance does and to WHICH handle.
    private static final FunctionalityId JOIN_A = FunctionalityId.forSagaFunctionality("joinTournamentA");
    private static final FunctionalityId JOIN_B = FunctionalityId.forSagaFunctionality("joinTournamentB");
    private static final FunctionalityId UPDATE_B = FunctionalityId.forSagaFunctionality("updateTournamentB");
    private static final FunctionalityId SUMMARY_A = FunctionalityId.forSagaFunctionality("generateSummaryA");
    private static final FunctionalityId SUMMARY_B = FunctionalityId.forSagaFunctionality("generateSummaryB");
    private static final FunctionalityId LEAVE_A = FunctionalityId.forSagaFunctionality("leaveTournamentA");
    private static final FunctionalityId MOVE_A_TO_STARTED = FunctionalityId
            .forSagaFunctionality("moveMemberToStartedTournament");
    private static final FunctionalityId REMOVE_A = FunctionalityId.forSagaFunctionality("removeTournamentA");
    private static final FunctionalityId UPDATE_LONELY_TOPIC = FunctionalityId
            .forSagaFunctionality("updateLonelyTopic");

    private @Nullable TestDriver driver;
    private @Nullable Oracle oracle;
    private @Nullable QuizzesTestFactory factory;
    private @Nullable FunctionalityCatalog catalog;

    private @Nullable Set<FunctionalityGroup> groups;

    @BeforeAll
    void setupAll() {
        driver = new TestDriver(QuizzesSimulator.class, List.of(), Path.of("target/planner-reports"))
                .setIterations(ITERATIONS);
        oracle = driver.getOracle();

        driver.init();

        factory = new QuizzesTestFactory(
                oracle.getBean(SagaUnitOfWorkService.class),
                oracle.getBean(ExecutionFunctionalities.class),
                oracle.getBean(UserFunctionalities.class),
                oracle.getBean(TopicFunctionalities.class),
                oracle.getBean(QuestionFunctionalities.class),
                oracle.getBean(TournamentFunctionalities.class));

        catalog = buildCatalog();

        // Profile + plan ONCE for the whole class: both tests read `groups`.
        Map<FunctionalityId, FunctionalityFootprint> footprints = driver.profileFunctionalities(catalog);
        for (FunctionalityFootprint footprint : footprints.values()) {
            log.info("footprint {} -> {}", footprint.functionalityId(), footprint.accesses());
        }

        groups = FunctionalityGroupPlanner.planGroups(footprints.values());
        log.info("planner produced {} group(s) out of a pool of {} functionalities ({} possible pairs):\n{}",
                groups.size(), footprints.size(), possiblePairs(footprints.size()),
                groups.stream().map(Object::toString).collect(Collectors.joining("\n")));
    }

    @AfterAll
    void tearDownAll() {
        if (driver != null) {
            driver.shutdown();
        }
    }

    /** Unordered pairs plus self-pairs: n*(n-1)/2 + n. */
    private static int possiblePairs(int poolSize) {
        return poolSize * (poolSize - 1) / 2 + poolSize;
    }

    /**
     * The shared initial state all catalog functionalities run on top of:
     * <ul>
     * <li>{@code tournamentA} — the base tournament, with {@code member}
     * enrolled (so it can be left / moved out of, and cannot be removed);</li>
     * <li>{@code tournamentB} — a second tournament of the SAME course
     * execution (quota scenarios + cross-handle contrast);</li>
     * <li>{@code startedTournament} — already running, so any enrolment in it
     * is rejected (the move's compensation subject);</li>
     * <li>{@code joiner} — a student in no tournament yet (quota joins);</li>
     * <li>{@code lonelyTopic} — a topic NO tournament references: updating it
     * emits an event nobody consumes, making it the disjoint control (the
     * regular {@code topic} is referenced by all three tournaments, so
     * updating THAT would fan out into tournament writes via event
     * handlers).</li>
     * </ul>
     * <p>
     * Every entry must survive its SOLO profiling run: {@link
     * TestDriver#profileFunctionalities} rejects a catalog whose solo run raises
     * a critical status. The move is the tight one — its enrolment in
     * {@code startedTournament} is rejected by design, so it only profiles
     * cleanly because it compensates cleanly on its own. Should that stop
     * holding, the whole class fails in {@code setupAll}, not in a test.
     */
    private FunctionalityCatalog buildCatalog() {
        SagaUnitOfWorkService unitOfWorkService = oracle.getBean(SagaUnitOfWorkService.class);
        CommandGateway gateway = oracle.getBean(CommandGateway.class);

        Supplier<AggregateHandlesRegistry> initialStateSetup = () -> {
            InitialState initialState = factory.setupInitialState();
            Integer executionId = initialState.courseExecutionDto().getAggregateId();
            Integer topicId = initialState.topicDto().getAggregateId();
            Integer creatorId = initialState.userDto().getAggregateId();
            Integer tournamentAId = initialState.tournamentDto().getAggregateId();

            TournamentDto tournamentB = factory.createTournament(
                    QuizzesTestFactory.TIME_1, QuizzesTestFactory.TIME_3, 1,
                    creatorId, executionId, List.of(topicId));

            TournamentDto startedTournament = factory.createStartedTournament(
                    creatorId, executionId, List.of(topicId));

            Integer joinerId = factory.createStudentInExecution(executionId,
                    QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2).getAggregateId();

            Integer memberId = factory.createStudentInExecution(executionId,
                    QuizzesTestFactory.USER_NAME_3, QuizzesTestFactory.USER_USERNAME_3).getAggregateId();
            factory.addParticipant(tournamentAId, executionId, memberId);

            TopicDto lonelyTopic = factory.createTopic(
                    initialState.courseExecutionDto(), QuizzesTestFactory.TOPIC_NAME_2);

            return new AggregateHandlesRegistry()
                    .register("execution", executionId)
                    .register("creator", creatorId)
                    .register("topic", topicId)
                    .register("question", initialState.questionDto().getAggregateId())
                    .register("tournamentA", tournamentAId)
                    .register("tournamentB", tournamentB.getAggregateId())
                    .register("startedTournament", startedTournament.getAggregateId())
                    .register("joiner", joinerId)
                    .register("member", memberId)
                    .register("lonelyTopic", lonelyTopic.getAggregateId());
        };

        Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories = new LinkedHashMap<>();

        factories.put(JOIN_A, registry -> factory.createAddParticipantWithinMaxTournamentsFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), registry.idOf("execution"),
                registry.idOf("joiner"), gateway));

        factories.put(JOIN_B, registry -> factory.createAddParticipantWithinMaxTournamentsFunctionality(
                unitOfWorkService, registry.idOf("tournamentB"), registry.idOf("execution"),
                registry.idOf("joiner"), gateway));

        factories.put(UPDATE_B, registry -> {
            TournamentDto updateDto = new TournamentDto();
            updateDto.setAggregateId(registry.idOf("tournamentB"));
            updateDto.setStartTime(DateHandler.toISOString(QuizzesTestFactory.TIME_1));
            updateDto.setEndTime(DateHandler.toISOString(QuizzesTestFactory.TIME_3));
            updateDto.setNumberOfQuestions(1);
            return factory.createUpdateTournamentFunctionality(
                    unitOfWorkService, updateDto, Set.of(registry.idOf("topic")), gateway);
        });

        factories.put(SUMMARY_A, registry -> factory.createGenerateTournamentSummaryFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), gateway));

        factories.put(SUMMARY_B, registry -> factory.createGenerateTournamentSummaryFunctionality(
                unitOfWorkService, registry.idOf("tournamentB"), gateway));

        factories.put(LEAVE_A, registry -> factory.createLeaveTournamentFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), registry.idOf("member"), gateway));

        factories.put(MOVE_A_TO_STARTED, registry -> factory.createMoveParticipantBetweenTournamentsFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), registry.idOf("startedTournament"),
                registry.idOf("execution"), registry.idOf("member"), gateway));

        factories.put(REMOVE_A, registry -> factory.createRemoveTournamentFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), gateway));

        factories.put(UPDATE_LONELY_TOPIC, registry -> {
            TopicDto updateDto = new TopicDto();
            updateDto.setAggregateId(registry.idOf("lonelyTopic"));
            updateDto.setName(QuizzesTestFactory.TOPIC_NAME_3);
            SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(
                    UpdateTopicFunctionalitySagas.class.getSimpleName());
            return new UpdateTopicFunctionalitySagas(unitOfWorkService, updateDto, unitOfWork, gateway);
        });

        return new FunctionalityCatalog(initialStateSetup, factories);
    }

    private FunctionalityGroup groupOf(FunctionalityId a, FunctionalityId b) {
        return groups.stream()
                .filter(group -> group.isPairOf(a, b))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "expected planner group (" + a + ", " + b + ") missing; got:\n"
                                + groups.stream().map(Object::toString).collect(Collectors.joining("\n"))));
    }

    private boolean hasGroup(FunctionalityId a, FunctionalityId b) {
        return groups.stream().anyMatch(group -> group.isPairOf(a, b));
    }

    // =============================================================================================
    // TEST A — profiling + planning: the right pairs out of the chaos
    // =============================================================================================

    @Test
    void plannerFindsTheKnownConflictPairsAndPrunesTheRest() {
        // The pairs behind every engineered anomaly of TestDriverQuizzesAppTest
        // must be found by the planner ON ITS OWN:
        // - the max-tournaments write skew (both joins of the same user),
        // - the summary non-repeatable read (summary vs the member leaving),
        // - the impossible compensation (move racing the removal of its source).
        assertTrue(hasGroup(JOIN_A, JOIN_B),
                "quota write-skew pair should be planned: the quota count READS every tournament of the "
                        + "execution while each join WRITES its own");
        assertTrue(hasGroup(SUMMARY_A, LEAVE_A),
                "summary non-repeatable-read pair should be planned (RW on tournamentA)");
        assertTrue(hasGroup(MOVE_A_TO_STARTED, REMOVE_A),
                "impossible-compensation pair should be planned (both touch tournamentA with writes)");
        assertTrue(hasGroup(JOIN_B, UPDATE_B),
                "WW pair on tournamentB should be planned");

        // Evidence spot check: the summary/leave conflict is on tournamentA.
        assertTrue(groupOf(SUMMARY_A, LEAVE_A).conflicts().stream()
                .anyMatch(conflict -> conflict.identity().equals("tournamentA")),
                "summary/leave evidence should name tournamentA, got: "
                        + groupOf(SUMMARY_A, LEAVE_A).conflicts());

        // Handle-level pruning: same aggregate TYPE, different tournaments.
        assertFalse(hasGroup(SUMMARY_B, LEAVE_A),
                "reading tournamentB must not pair with writing tournamentA");
        assertFalse(hasGroup(UPDATE_B, LEAVE_A),
                "writing tournamentB must not pair with writing tournamentA");
        assertFalse(hasGroup(SUMMARY_A, SUMMARY_B),
                "two read-only summaries of different tournaments must not pair");

        // Read-read pruning: both joins read the execution and summaries read
        // tournaments, but reads alone are never a conflict.
        assertFalse(hasGroup(SUMMARY_B, JOIN_A),
                "joinA only READS tournamentB (quota count) and summaryB only reads it too: RR, no pair");

        // Self-pairs: writers race a same-arguments copy of themselves ...
        assertTrue(hasGroup(JOIN_A, JOIN_A), "a join should self-pair (duplicate-request race)");
        // ... read-only functionalities do not.
        assertFalse(hasGroup(SUMMARY_A, SUMMARY_A), "a read-only summary should not self-pair");
        assertFalse(hasGroup(SUMMARY_B, SUMMARY_B));

        // Disjoint control: the lonely topic touches nothing anyone else does.
        assertTrue(groups.stream()
                .filter(group -> group.members().contains(UPDATE_LONELY_TOPIC))
                .allMatch(FunctionalityGroup::isSelfPair),
                "updateLonelyTopic should at most self-pair, never pair with the tournament crowd");

        // Every group must justify itself.
        assertTrue(groups.stream().noneMatch(group -> group.conflicts().isEmpty()),
                "every planned group should carry conflict evidence");

        // And the whole point: the plan must be smaller than brute force.
        int allPairs = possiblePairs(catalog.funcFactories().size());
        log.info("pruning: {} planned group(s) vs {} possible pairs", groups.size(), allPairs);
        assertTrue(groups.size() < allPairs,
                "planner should prune at least some of the " + allPairs + " possible pairs");
    }

    // =============================================================================================
    // TEST B — the planner's own groups retain detection power through the driver
    // =============================================================================================

    @Test
    void exploringPlannerChosenGroupsSurfacesTheKnownAnomalies() {
        // Quota write skew: some interleaving must break the max-tournaments
        // inter-invariant (or equivalently surface the write skew).
        List<TestResult> quotaRuns = driver.exploreGroup(catalog, groupOf(JOIN_A, JOIN_B));
        assertEquals(ITERATIONS, quotaRuns.size());
        assertTrue(quotaRuns.stream().anyMatch(result -> result.statuses()
                .contains(TestStatus.INTER_INVARIANT_VIOLATION)
                || result.anomalies().stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.WRITE_SKEW)),
                "exploring the planner's quota pair should surface the write skew / quota violation");

        // Summary non-repeatable read.
        List<TestResult> summaryRuns = driver.exploreGroup(catalog, groupOf(SUMMARY_A, LEAVE_A));
        assertEquals(ITERATIONS, summaryRuns.size());
        assertTrue(summaryRuns.stream().anyMatch(result -> result.anomalies().stream()
                .anyMatch(anomaly -> anomaly.type() == AnomalyType.NON_REPEATABLE_READ)),
                "exploring the planner's summary/leave pair should surface the non-repeatable read");

        // Impossible compensation: dirty read + critical step failure.
        List<TestResult> moveRuns = driver.exploreGroup(catalog, groupOf(MOVE_A_TO_STARTED, REMOVE_A));
        assertEquals(ITERATIONS, moveRuns.size());
        assertTrue(moveRuns.stream().anyMatch(result -> result.statuses()
                .contains(TestStatus.CRITICAL_STEP_FAILURE)
                || result.anomalies().stream().anyMatch(anomaly -> anomaly.type() == AnomalyType.DIRTY_READ)),
                "exploring the planner's move/remove pair should strand the compensation in some interleaving");

        // A self-pair must be runnable end-to-end: two same-arguments instances
        // of the same functionality, distinct ids, full budget.
        List<TestResult> selfRuns = driver.exploreGroup(catalog, groupOf(JOIN_A, JOIN_A));
        assertEquals(ITERATIONS, selfRuns.size(),
                "self-pair exploration should run the full budget (two instances, same arguments)");
        assertTrue(selfRuns.stream().noneMatch(result -> result.statuses()
                .contains(TestStatus.INTERNAL_SYSTEM_EXCEPTION)),
                "instantiating the same functionality twice should not break the driver itself");
        long selfFindings = selfRuns.stream()
                .filter(result -> !result.exceptions().isEmpty() || !result.statuses().isEmpty())
                .count();
        log.info("self-pair joinTournamentA x2: {} of {} runs had exceptions or statuses",
                selfFindings, selfRuns.size());
    }
}
