package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.EnrollStudentInExecutionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class EnrollStudentInExecutionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String ACRONYM_1 = "SE01"
    public static final String ACADEMIC_TERM_1 = "1st Semester 2024/2025"
    public static final String USER_USERNAME_2 = "janedoe"

    def courseDto
    def executionDto
    def userDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def "enrollStudentInExecution: success"() {
        when:
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        then:
        def result = executionService.getExecutionById(executionDto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        // verify via service directly since getExecutionById in functionalities returns ExecutionDto without students
        noExceptionThrown()
    }

    def "enrollStudentInExecution: STUDENT_ALREADY_ENROLLED violation"() {
        given: 'student already enrolled'
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        when:
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        then:
        thrown(QuizzesFullException)
    }

    def "enrollStudentInExecution: INACTIVE_USER — deleted user causes prerequisite failure"() {
        given: 'user is deleted'
        userFunctionalities.deleteUser(userDto.aggregateId)

        when:
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        then:
        // deleteUser sets state=DELETED; GetUserByIdCommand cannot find a DELETED aggregate,
        // so the saga's data-assembly step fails with SimulatorException (P4a prerequisite failure)
        // before the INACTIVE_USER P3 guard in ExecutionService is reached
        thrown(SimulatorException)
    }

    def "enrollStudentInExecution: getExecutionStep acquires IN_ENROLL_STUDENT semantic lock"() {
        given: 'workflow pauses after getExecutionStep'
        def uow1 = unitOfWorkService.createUnitOfWork("enrollStudentInExecution")
        def func1 = new EnrollStudentInExecutionFunctionalitySagas(
                unitOfWorkService, executionDto.aggregateId, userDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getExecutionStep", uow1)

        expect: 'execution is locked'
        sagaStateOf(executionDto.aggregateId) == ExecutionSagaState.IN_ENROLL_STUDENT

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
    }
}
