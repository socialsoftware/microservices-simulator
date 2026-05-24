package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.CreateQuizAnswerFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.UpdateStudentNameFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateStudentNameTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String ACRONYM_1 = "SE01"
    public static final String ACADEMIC_TERM_1 = "1st Semester 2024/2025"
    public static final String NEW_NAME = "John Updated"

    def courseDto
    def executionDto
    def userDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)
    }

    def "updateStudentName: success"() {
        when:
        executionFunctionalities.updateStudentName(executionDto.aggregateId, userDto.aggregateId, NEW_NAME)

        then:
        noExceptionThrown()

        and: 'user name is updated in User aggregate'
        def updatedUser = userFunctionalities.getUserById(userDto.aggregateId)
        updatedUser.name == NEW_NAME

        and: 'cached student name in execution is updated'
        def student = executionFunctionalities.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId)
        student.userName == NEW_NAME
    }

    def "updateStudentName: getExecutionStep acquires IN_UPDATE_STUDENT_NAME semantic lock"() {
        given: 'workflow pauses after getExecutionStep'
        def uow1 = unitOfWorkService.createUnitOfWork("updateStudentName")
        def func1 = new UpdateStudentNameFunctionalitySagas(
                unitOfWorkService, executionDto.aggregateId, userDto.aggregateId, NEW_NAME, uow1, commandGateway)
        func1.executeUntilStep("getExecutionStep", uow1)

        expect: 'execution is locked'
        sagaStateOf(executionDto.aggregateId) == ExecutionSagaState.IN_UPDATE_STUDENT_NAME

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
    }
    def "updateStudentName: updateUserNameStep sees forbidden state when user is locked by concurrent createQuizAnswer"() {
        given:
        def quiz = createQuiz(executionDto.aggregateId, [])
        def uow1 = unitOfWorkService.createUnitOfWork("updateStudentName")
        def func1 = new UpdateStudentNameFunctionalitySagas(
                unitOfWorkService, executionDto.aggregateId, userDto.aggregateId, NEW_NAME, uow1, commandGateway)
        func1.executeUntilStep("updateStudentNameInExecutionStep", uow1)

        and: 'concurrent createQuizAnswer acquires READ_USER on the same user'
        def uow2 = unitOfWorkService.createUnitOfWork("createQuizAnswer")
        def func2 = new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, quiz.aggregateId, userDto.aggregateId, uow2, commandGateway)
        func2.executeUntilStep("getUserStep", uow2)

        when: 'updateStudentName resumes into the forbidden user state'
        func1.resumeWorkflow(uow1)

        then:
        thrown(SimulatorException)
    }
}
