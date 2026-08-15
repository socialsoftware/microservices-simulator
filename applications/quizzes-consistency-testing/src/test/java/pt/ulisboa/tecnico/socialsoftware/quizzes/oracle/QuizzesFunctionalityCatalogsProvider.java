package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities;

@Component
@Profile("oracle")
public class QuizzesFunctionalityCatalogsProvider implements FunctionalityCatalogsProvider {

    private static final String TOURNAMENTS_CATALOG = "tournaments";

    // Names say what each instance does and to WHICH handle: these ids are how
    // the planner's groups, the reports and the findings refer back to them.
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

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway gateway;
    @Autowired
    private ExecutionFunctionalities executionFunctionalities;
    @Autowired
    private UserFunctionalities userFunctionalities;
    @Autowired
    private TopicFunctionalities topicFunctionalities;
    @Autowired
    private QuestionFunctionalities questionFunctionalities;
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities;

    @Override
    public List<FunctionalityCatalog> getCatalogs() {
        return List.of(new FunctionalityCatalog(
                TOURNAMENTS_CATALOG, initialStateSetup(), functionalityFactories()));
    }

    private Supplier<AggregateHandlesRegistry> initialStateSetup() {
        return () -> {
            QuizzesTestFactory factory = testFactory();

            InitialState initialState = factory.setupInitialState();
            Integer executionId = initialState.courseExecutionDto().getAggregateId();
            Integer topicId = initialState.topicDto().getAggregateId();
            Integer creatorId = initialState.userDto().getAggregateId();
            Integer tournamentAId = initialState.tournamentDto().getAggregateId();

            // A second tournament of the SAME execution: quota scenarios and the
            // cross-handle contrast that proves the planner prunes by handle.
            TournamentDto tournamentB = factory.createTournament(
                    QuizzesTestFactory.time1(), QuizzesTestFactory.time3(), 1,
                    creatorId, executionId, List.of(topicId));

            // Already running, so any enrolment in it is rejected: the move's
            // compensation subject.
            TournamentDto startedTournament = factory.createStartedTournament(
                    creatorId, executionId, List.of(topicId));

            Integer joinerId = factory.createStudentInExecution(executionId,
                    QuizzesTestFactory.USER_NAME_2, QuizzesTestFactory.USER_USERNAME_2)
                    .getAggregateId();

            Integer memberId = factory.createStudentInExecution(executionId,
                    QuizzesTestFactory.USER_NAME_3, QuizzesTestFactory.USER_USERNAME_3)
                    .getAggregateId();
            factory.addParticipant(tournamentAId, executionId, memberId);

            // Referenced by no tournament: updating it emits an event nobody
            // consumes, unlike the regular topic, whose update fans out into
            // tournament writes through event handlers.
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
    }

    private Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> functionalityFactories() {
        Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> factories = new LinkedHashMap<>();

        factories.put(JOIN_A, registry -> testFactory().createAddParticipantWithinMaxTournamentsFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), registry.idOf("execution"),
                registry.idOf("joiner"), gateway));

        factories.put(JOIN_B, registry -> testFactory().createAddParticipantWithinMaxTournamentsFunctionality(
                unitOfWorkService, registry.idOf("tournamentB"), registry.idOf("execution"),
                registry.idOf("joiner"), gateway));

        factories.put(UPDATE_B, registry -> {
            TournamentDto updateDto = new TournamentDto();
            updateDto.setAggregateId(registry.idOf("tournamentB"));
            updateDto.setStartTime(DateHandler.toISOString(QuizzesTestFactory.time1()));
            updateDto.setEndTime(DateHandler.toISOString(QuizzesTestFactory.time3()));
            updateDto.setNumberOfQuestions(1);
            return testFactory().createUpdateTournamentFunctionality(
                    unitOfWorkService, updateDto, Set.of(registry.idOf("topic")), gateway);
        });

        factories.put(SUMMARY_A, registry -> testFactory().createGenerateTournamentSummaryFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), gateway));

        factories.put(SUMMARY_B, registry -> testFactory().createGenerateTournamentSummaryFunctionality(
                unitOfWorkService, registry.idOf("tournamentB"), gateway));

        factories.put(LEAVE_A, registry -> testFactory().createLeaveTournamentFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), registry.idOf("member"), gateway));

        factories.put(MOVE_A_TO_STARTED,
                registry -> testFactory().createMoveParticipantBetweenTournamentsFunctionality(
                        unitOfWorkService, registry.idOf("tournamentA"),
                        registry.idOf("startedTournament"),
                        registry.idOf("execution"), registry.idOf("member"), gateway));

        factories.put(REMOVE_A, registry -> testFactory().createRemoveTournamentFunctionality(
                unitOfWorkService, registry.idOf("tournamentA"), gateway));

        factories.put(UPDATE_LONELY_TOPIC, registry -> {
            TopicDto updateDto = new TopicDto();
            updateDto.setAggregateId(registry.idOf("lonelyTopic"));
            updateDto.setName(QuizzesTestFactory.TOPIC_NAME_3);
            SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(
                    UpdateTopicFunctionalitySagas.class.getSimpleName());
            return new UpdateTopicFunctionalitySagas(unitOfWorkService, updateDto, unitOfWork, gateway);
        });

        return factories;
    }

    /**
     * A factory over the currently injected beans. Built on demand rather than
     * cached because the tool may replace beans (it overrides the unit-of-work
     * service and the event application service) between runs.
     */
    private QuizzesTestFactory testFactory() {
        return new QuizzesTestFactory(
                unitOfWorkService,
                executionFunctionalities,
                userFunctionalities,
                topicFunctionalities,
                questionFunctionalities,
                tournamentFunctionalities);
    }
}
