package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.DisenrollStudentFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DisenrollStudentTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String ACRONYM_1 = "SE01"
    public static final String ACADEMIC_TERM_1 = "1st Semester 2024/2025"

    def courseDto
    def executionDto
    def userDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)
    }

    def "disenrollStudent: success"() {
        when:
        executionFunctionalities.disenrollStudent(executionDto.aggregateId, userDto.aggregateId)

        then:
        noExceptionThrown()
    }

    def "disenrollStudent: getExecutionStep acquires IN_DISENROLL_STUDENT semantic lock"() {
        given: 'workflow pauses after getExecutionStep'
        def uow1 = unitOfWorkService.createUnitOfWork("disenrollStudent")
        def func1 = new DisenrollStudentFunctionalitySagas(
                unitOfWorkService, executionDto.aggregateId, userDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getExecutionStep", uow1)

        expect: 'execution is locked'
        sagaStateOf(executionDto.aggregateId) == ExecutionSagaState.IN_DISENROLL_STUDENT

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
    }
}
