package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorDomainException
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

import java.util.concurrent.CompletionException

@DataJpaTest
class RemoveTournamentAddParticipantRecoveryWindowExploratoryTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities
    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    private UserDto creatorDto
    private UserDto participantDto
    private TopicDto topicDto1
    private TopicDto topicDto2
    private QuestionDto questionDto1
    private QuestionDto questionDto2
    private TournamentDto tournamentDto
    private RecordingInvariantRecorder invariantRecorder
    private DynamicEvidenceRecorderHolder.Scope invariantRecorderScope

    def setup() {
        FaultVectorProviderHolder.clear()
        invariantRecorder = new RecordingInvariantRecorder(DynamicEvidenceRecorderHolder.recorder)
        invariantRecorderScope = DynamicEvidenceRecorderHolder.install(invariantRecorder)

        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE,
                COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        creatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        participantDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, creatorDto.aggregateId)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, participantDto.aggregateId)

        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        questionDto1 = createQuestion(courseExecutionDto, [topicDto1] as Set, TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, [topicDto2] as Set, TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        tournamentDto = createTournament(TIME_1, TIME_3, 2, creatorDto.aggregateId,
                courseExecutionDto.aggregateId, [topicDto1.aggregateId, topicDto2.aggregateId])
    }

    def cleanup() {
        FaultVectorProviderHolder.clear()
        invariantRecorderScope?.close()
    }

    def 'control: participant-free RemoveTournament deletes Tournament and Quiz with no ImpactV1 signal'() {
        given:
        def remove = newRemoveTournament()
        def quizId = tournamentDto.quiz.aggregateId

        when:
        executeRemoveStep(remove, 'getTournamentStep')
        executeRemoveStep(remove, 'removeQuizStep')
        executeRemoveStep(remove, 'removeTournamentStep')
        def finalization = remove.functionality.finalizeForExecutor(remove.unitOfWork)

        then:
        finalization.success()
        remove.unitOfWork.executedSteps == ['getTournamentStep', 'removeQuizStep', 'removeTournamentStep']
        def deletedTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        deletedTournament.state == Aggregate.AggregateState.DELETED
        deletedTournament.sagaState == GenericSagaState.NOT_IN_SAGA
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
        invariantRecorder.invariantViolations.empty
    }

    def 'forward interleaving: AddParticipant after RemoveTournament read makes final removal violate DELETE invariant'() {
        given:
        def remove = newRemoveTournament()
        def add = newAddParticipant()
        def quizId = tournamentDto.quiz.aggregateId

        when:
        executeRemoveStep(remove, 'getTournamentStep')
        executeAddParticipant(add)
        executeRemoveStep(remove, 'removeQuizStep')
        def removalResult = remove.functionality.executeStepForExecutorControlled(
                'removeTournamentStep', remove.unitOfWork)

        then:
        !removalResult.completed()
        removalResult.failure() instanceof QuizzesException
        remove.unitOfWork.executedSteps == ['getTournamentStep', 'removeQuizStep', 'removeTournamentStep']
        add.unitOfWork.executedSteps == ['getUserStep', 'addParticipantStep']
        invariantRecorder.invariantViolations.size() == 1
        invariantRecorder.invariantViolations[0].payload.aggregateType == 'SagaTournament'
        invariantRecorder.invariantViolations[0].payload.aggregateId == tournamentDto.aggregateId.toString()
        invariantRecorder.invariantViolations[0].functionalityName == RemoveTournamentFunctionalitySagas.simpleName
        invariantRecorder.invariantViolations[0].stepName == 'removeTournamentStep'

        when: 'the only available recovery rolls back the Tournament semantic lock'
        def checkpoints = remove.functionality.recoveryCheckpointsForExecutor(remove.unitOfWork)
        def recovery = remove.functionality.recoverStepForExecutor('getTournamentStep', remove.unitOfWork)

        then:
        checkpoints*.sourceStepName() == ['getTournamentStep']
        !recovery.explicitCompensationExecuted()
        recovery.implicitRollbackExecuted()
        def survivingTournament = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        survivingTournament.participants*.aggregateId == [participantDto.aggregateId]
        sagaStateOf(tournamentDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
    }

    def 'assigned AddParticipant fault suppresses the invariant and lets RemoveTournament finish safely'() {
        given:
        def remove = newRemoveTournament()
        def add = newAddParticipant()
        def quizId = tournamentDto.quiz.aggregateId
        executeRemoveStep(remove, 'getTournamentStep')
        executeAddStep(add, 'getUserStep')

        when:
        def fault = injectFault(add.functionality, add.unitOfWork, 'addParticipantStep', 1)

        then:
        fault instanceof FaultVectorInjectedFaultException
        fault.runtimeStepName == 'addParticipantStep'
        add.unitOfWork.executedSteps == ['getUserStep']
        add.functionality.recoveryCheckpointsForExecutor(add.unitOfWork).empty

        when:
        add.functionality.resumeCompensation(add.unitOfWork)
        executeRemoveStep(remove, 'removeQuizStep')
        executeRemoveStep(remove, 'removeTournamentStep')
        def finalization = remove.functionality.finalizeForExecutor(remove.unitOfWork)

        then:
        finalization.success()
        unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId).state == Aggregate.AggregateState.DELETED
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
        invariantRecorder.invariantViolations.empty
    }

    def 'control forward order: completed removal rejects the later AddParticipant'() {
        given:
        def remove = newRemoveTournament()
        def add = newAddParticipant()
        def quizId = tournamentDto.quiz.aggregateId
        executeRemoveStep(remove, 'getTournamentStep')
        executeRemoveStep(remove, 'removeQuizStep')
        executeRemoveStep(remove, 'removeTournamentStep')
        assert remove.functionality.finalizeForExecutor(remove.unitOfWork).success()
        executeAddStep(add, 'getUserStep')

        when:
        add.functionality.executeStepForExecutor('addParticipantStep', add.unitOfWork)

        then:
        def failure = thrown(CompletionException)
        failure.cause instanceof SimulatorDomainException
        unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId).state == Aggregate.AggregateState.DELETED
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
        invariantRecorder.invariantViolations.empty
    }

    def 'assigned final-removal fault and recovery let AddParticipant create a Tournament pointing to a deleted Quiz'() {
        given:
        def remove = newRemoveTournament()
        def add = newAddParticipant()
        def quizId = tournamentDto.quiz.aggregateId
        executeRemoveStep(remove, 'getTournamentStep')
        executeRemoveStep(remove, 'removeQuizStep')

        when:
        def fault = injectFault(remove.functionality, remove.unitOfWork, 'removeTournamentStep', 2)

        then:
        fault instanceof FaultVectorInjectedFaultException
        fault.runtimeStepName == 'removeTournamentStep'
        remove.unitOfWork.executedSteps == ['getTournamentStep', 'removeQuizStep']
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED

        when:
        def checkpoints = remove.functionality.recoveryCheckpointsForExecutor(remove.unitOfWork)
        def recovery = remove.functionality.recoverStepForExecutor('getTournamentStep', remove.unitOfWork)
        executeAddParticipant(add)

        then:
        checkpoints*.sourceStepName() == ['getTournamentStep']
        !recovery.explicitCompensationExecuted()
        recovery.implicitRollbackExecuted()
        def survivingTournament = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        survivingTournament.participants*.aggregateId == [participantDto.aggregateId]
        survivingTournament.quiz.aggregateId == quizId
        sagaStateOf(tournamentDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
        unitOfWorkService.aggregateDeletedLoad(quizId).state == Aggregate.AggregateState.DELETED
        invariantRecorder.invariantViolations.empty
    }

    private Map newRemoveTournament() {
        def unitOfWork = unitOfWorkService.createUnitOfWork(RemoveTournamentFunctionalitySagas.simpleName)
        def functionality = new RemoveTournamentFunctionalitySagas(
                unitOfWorkService, tournamentDto.aggregateId, unitOfWork, commandGateway)
        [functionality: functionality, unitOfWork: unitOfWork]
    }

    private Map newAddParticipant() {
        def unitOfWork = unitOfWorkService.createUnitOfWork(AddParticipantFunctionalitySagas.simpleName)
        def functionality = new AddParticipantFunctionalitySagas(
                unitOfWorkService,
                tournamentDto.aggregateId,
                courseExecutionDto.aggregateId,
                participantDto.aggregateId,
                unitOfWork,
                commandGateway)
        [functionality: functionality, unitOfWork: unitOfWork]
    }

    private static void executeRemoveStep(Map remove, String stepName) {
        remove.functionality.executeStepForExecutor(stepName, remove.unitOfWork)
    }

    private static void executeAddStep(Map add, String stepName) {
        add.functionality.executeStepForExecutor(stepName, add.unitOfWork)
    }

    private static void executeAddParticipant(Map add) {
        executeAddStep(add, 'getUserStep')
        executeAddStep(add, 'addParticipantStep')
        assert add.functionality.finalizeForExecutor(add.unitOfWork).success()
    }

    private static Throwable injectFault(def functionality, def unitOfWork, String stepName, int slotIndex) {
        def context = new FaultVectorBoundaryContext(
                'remove-add-exploratory-attempt',
                'remove-tournament-add-participant-window',
                functionality.class.simpleName,
                "${stepName}-occurrence",
                slotIndex,
                functionality.class.name,
                functionality.class.simpleName,
                stepName,
                1)
        def providerScope = FaultVectorProviderHolder.install(
                new InMemoryFaultVectorProvider([(slotIndex): FaultVectorFault.from(context)]))
        def boundaryScope = FaultVectorProviderHolder.enterBoundary(context)
        try {
            functionality.executeStepForExecutor(stepName, unitOfWork)
            throw new AssertionError("Expected assigned ${stepName} fault")
        } catch (CompletionException failure) {
            return failure.cause
        } finally {
            boundaryScope.close()
            providerScope.close()
        }
    }

    private static class RecordingInvariantRecorder implements DynamicEvidenceRecorder {
        private final DynamicEvidenceRecorder delegate
        private final List<DynamicEvidenceEvent> invariantViolations = []

        private RecordingInvariantRecorder(DynamicEvidenceRecorder delegate) {
            this.delegate = delegate
        }

        @Override
        boolean isEnabled() {
            true
        }

        @Override
        void record(DynamicEvidenceEvent event) {
            if (event.eventKind == 'INVARIANT_VIOLATION') {
                invariantViolations.add(event)
            }
            delegate.record(event)
        }

        @Override
        void close() {
            // The recorder installed by the test/application context owns its lifecycle.
        }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
