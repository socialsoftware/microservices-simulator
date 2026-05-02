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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.UpdateExecutionFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateExecutionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    public static final String ACRONYM_1 = "SE01"
    public static final String ACRONYM_2 = "SE02"
    public static final String ACADEMIC_TERM_1 = "1st Semester 2024/2025"
    public static final String ACADEMIC_TERM_2 = "2nd Semester 2024/2025"

    def courseDto
    def executionDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
    }

    def "updateExecution: success"() {
        when:
        executionFunctionalities.updateExecution(executionDto.aggregateId, ACRONYM_2, ACADEMIC_TERM_2)

        then:
        def result = executionFunctionalities.getExecutionById(executionDto.aggregateId)
        result.acronym == ACRONYM_2
        result.academicTerm == ACADEMIC_TERM_2
    }

    def "updateExecution: getExecutionStep acquires IN_UPDATE_EXECUTION semantic lock"() {
        given: 'workflow pauses after getExecutionStep'
        def uow1 = unitOfWorkService.createUnitOfWork("updateExecution")
        def func1 = new UpdateExecutionFunctionalitySagas(
                unitOfWorkService, executionDto.aggregateId, ACRONYM_2, ACADEMIC_TERM_2, uow1, commandGateway)
        func1.executeUntilStep("getExecutionStep", uow1)

        expect: 'execution is locked'
        sagaStateOf(executionDto.aggregateId) == ExecutionSagaState.IN_UPDATE_EXECUTION

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()

        and: 'execution was updated'
        def result = executionFunctionalities.getExecutionById(executionDto.aggregateId)
        result.acronym == ACRONYM_2
        result.academicTerm == ACADEMIC_TERM_2
    }
}
