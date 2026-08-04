package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveStudentFromCourseExecutionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class SagaStatePersistenceTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private LocalCommandGateway commandGateway

    private CourseExecutionDto courseExecutionDto
    private UserDto userDto

    def setup() {
        given: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: 'a student enrolled in it'
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)
    }

    def cleanup() {
    }

    def "saga state persisted as NOT_IN_SAGA is read back correctly when forced to load from db"() {
        when: 'the saga state is loaded straight from the db (persistence context cleared)'
        def stateFromDb = sagaStateOfFromDb(courseExecutionDto.getAggregateId())

        then: 'it matches the expected state and the cached read'
        stateFromDb == GenericSagaState.NOT_IN_SAGA
        stateFromDb == sagaStateOf(courseExecutionDto.getAggregateId())
    }

    def "saga state change is persisted and read back correctly when forced to load from db"() {
        given: 'the aggregate saga state is set to IN_SAGA'
        def uow = unitOfWorkService.createUnitOfWork("TEST")
        unitOfWorkService.registerSagaState(courseExecutionDto.getAggregateId(), GenericSagaState.IN_SAGA, uow)

        when: 'the saga state is loaded straight from the db (persistence context cleared)'
        def stateFromDb = sagaStateOfFromDb(courseExecutionDto.getAggregateId())

        then: 'the persisted state is found'
        stateFromDb == GenericSagaState.IN_SAGA
    }

    def "intermediate saga lock from a non-generic state class is persisted and read back from db"() {
        given: 'a remove-student saga paused after acquiring the read lock on the course execution'
        def uow = unitOfWorkService.createUnitOfWork(RemoveStudentFromCourseExecutionFunctionalitySagas.class.getSimpleName())
        def removeStudentFunctionality = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                unitOfWorkService, courseExecutionDto.aggregateId, userDto.aggregateId, uow, commandGateway)
        removeStudentFunctionality.executeUntilStep('getOldCourseExecutionStep', uow)

        when: 'the saga state is loaded straight from the db (persistence context cleared)'
        def stateFromDb = sagaStateOfFromDb(courseExecutionDto.aggregateId)

        then: 'the intermediate READ_COURSE lock (CourseExecutionSagaState) is found'
        stateFromDb == CourseExecutionSagaState.READ_COURSE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
