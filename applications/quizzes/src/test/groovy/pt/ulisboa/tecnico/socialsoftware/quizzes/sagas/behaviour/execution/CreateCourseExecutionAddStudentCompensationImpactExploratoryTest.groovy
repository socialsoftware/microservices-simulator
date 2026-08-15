package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryException
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
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.AddStudentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.CreateCourseExecutionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

import java.util.concurrent.CompletionException

@DataJpaTest
class CreateCourseExecutionAddStudentCompensationImpactExploratoryTest extends QuizzesSpockTest {
    private static final String NEW_COURSE = 'COMPENSATION_WINDOW_COURSE'
    private static final String NEW_ACRONYM = 'COMP_WINDOW'
    private static final List<String> CREATE_PREFIX = [
            'getCourseStep',
            'createCourseStep',
            'createCourseExecutionStep'
    ]

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private CourseExecutionRepository courseExecutionRepository
    @Autowired
    private LocalCommandGateway commandGateway

    private UserDto studentDto
    private RecordingInvariantRecorder invariantRecorder
    private DynamicEvidenceRecorderHolder.Scope invariantRecorderScope

    def setup() {
        FaultVectorProviderHolder.clear()
        invariantRecorder = new RecordingInvariantRecorder(DynamicEvidenceRecorderHolder.recorder)
        invariantRecorderScope = DynamicEvidenceRecorderHolder.install(invariantRecorder)
        studentDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def cleanup() {
        FaultVectorProviderHolder.clear()
        invariantRecorderScope?.close()
    }

    def 'control: enrolled newly created execution completes without assigned fault or ImpactV1 signal'() {
        given:
        def create = newCreateCourseExecution()
        executeCreatePrefix(create)
        def executionId = create.functionality.createdCourseExecution.aggregateId
        def courseId = create.functionality.courseExecutionDto.courseAggregateId
        def add = newAddStudent(executionId)

        when:
        executeAddStudent(add)
        executeCreateStep(create, 'updateCourseExecutionCountStep')
        def finalization = create.functionality.finalizeForExecutor(create.unitOfWork)

        then:
        finalization.success()
        create.unitOfWork.executedSteps == CREATE_PREFIX + ['updateCourseExecutionCountStep']
        add.unitOfWork.executedSteps == ['getUserStep', 'enrollStudentStep']
        def execution = courseExecutionRepository.findLastAggregateVersion(executionId).orElseThrow()
        execution.state == Aggregate.AggregateState.ACTIVE
        execution.students*.userAggregateId == [studentDto.aggregateId]
        ((SagaAggregate) execution).sagaState == GenericSagaState.NOT_IN_SAGA
        def course = loadCourse(courseId)
        course.state == Aggregate.AggregateState.ACTIVE
        course.courseExecutionCount == 1
        ((SagaAggregate) course).sagaState == GenericSagaState.NOT_IN_SAGA
        invariantRecorder.invariantViolations.empty
    }

    def 'assigned final-step fault makes createCourseExecutionStep compensation violate REMOVE_NO_STUDENTS'() {
        given:
        def create = newCreateCourseExecution()
        executeCreatePrefix(create)
        def executionId = create.functionality.createdCourseExecution.aggregateId
        def courseId = create.functionality.courseExecutionDto.courseAggregateId
        def add = newAddStudent(executionId)
        executeAddStudent(add)

        when:
        def fault = injectCreateFault(create, 'updateCourseExecutionCountStep', 3)

        then:
        fault instanceof FaultVectorInjectedFaultException
        fault.runtimeStepName == 'updateCourseExecutionCountStep'
        create.unitOfWork.executedSteps == CREATE_PREFIX
        invariantRecorder.invariantViolations.empty

        when:
        def checkpoints = create.functionality.recoveryCheckpointsForExecutor(create.unitOfWork)
        create.functionality.recoverStepForExecutor('createCourseExecutionStep', create.unitOfWork)

        then:
        checkpoints*.sourceStepName() == ['createCourseExecutionStep', 'createCourseStep']
        def recoveryFailure = thrown(WorkflowStepRecoveryException)
        recoveryFailure.failedRecoveryKind() == 'EXPLICIT_COMPENSATION'
        !recoveryFailure.completedRecovery().explicitCompensationExecuted()
        !recoveryFailure.completedRecovery().implicitRollbackExecuted()
        recoveryFailure.cause.class.simpleName == 'QuizzesException'
        invariantRecorder.invariantViolations.size() == 1
        invariantRecorder.invariantViolations[0].payload.aggregateType == 'SagaExecution'
        invariantRecorder.invariantViolations[0].payload.aggregateId == executionId.toString()
        invariantRecorder.invariantViolations[0].functionalityName == CreateCourseExecutionFunctionalitySagas.simpleName
        invariantRecorder.invariantViolations[0].stepName == null

        and: 'failed compensation leaves the normally created aggregates active'
        def execution = courseExecutionRepository.findLastAggregateVersion(executionId).orElseThrow()
        execution.state == Aggregate.AggregateState.ACTIVE
        execution.students*.userAggregateId == [studentDto.aggregateId]
        ((SagaAggregate) execution).sagaState == GenericSagaState.NOT_IN_SAGA
        def course = loadCourse(courseId)
        course.state == Aggregate.AggregateState.ACTIVE
        course.courseExecutionCount == 0
        ((SagaAggregate) course).sagaState == GenericSagaState.NOT_IN_SAGA
        !create.unitOfWork.isCompensationExecuted('createCourseExecutionStep')
        !create.unitOfWork.isCompensationExecuted('createCourseStep')
    }

    private Map newCreateCourseExecution() {
        def input = new CourseExecutionDto(
                name: NEW_COURSE,
                type: COURSE_EXECUTION_TYPE,
                acronym: NEW_ACRONYM,
                academicTerm: COURSE_EXECUTION_ACADEMIC_TERM,
                endDate: DateHandler.toISOString(TIME_4))
        def unitOfWork = unitOfWorkService.createUnitOfWork(CreateCourseExecutionFunctionalitySagas.simpleName)
        def functionality = new CreateCourseExecutionFunctionalitySagas(
                unitOfWorkService, input, unitOfWork, commandGateway)
        [functionality: functionality, unitOfWork: unitOfWork]
    }

    private Map newAddStudent(Integer executionId) {
        def unitOfWork = unitOfWorkService.createUnitOfWork(AddStudentFunctionalitySagas.simpleName)
        def functionality = new AddStudentFunctionalitySagas(
                unitOfWorkService, executionId, studentDto.aggregateId, unitOfWork, commandGateway)
        [functionality: functionality, unitOfWork: unitOfWork]
    }

    private static void executeCreatePrefix(Map create) {
        CREATE_PREFIX.each { executeCreateStep(create, it) }
    }

    private static void executeCreateStep(Map create, String stepName) {
        create.functionality.executeStepForExecutor(stepName, create.unitOfWork)
    }

    private static void executeAddStudent(Map add) {
        add.functionality.executeStepForExecutor('getUserStep', add.unitOfWork)
        add.functionality.executeStepForExecutor('enrollStudentStep', add.unitOfWork)
        assert add.functionality.finalizeForExecutor(add.unitOfWork).success()
    }

    private Course loadCourse(Integer courseId) {
        def inspectionUnitOfWork = unitOfWorkService.createUnitOfWork('inspectCourse')
        (Course) unitOfWorkService.aggregateLoadAndRegisterRead(courseId, inspectionUnitOfWork)
    }

    private static Throwable injectCreateFault(Map create, String stepName, int slotIndex) {
        def functionality = create.functionality
        def context = new FaultVectorBoundaryContext(
                'create-execution-compensation-attempt',
                'create-course-execution-add-student-window',
                'create-course-execution-1',
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
            functionality.executeStepForExecutor(stepName, create.unitOfWork)
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
