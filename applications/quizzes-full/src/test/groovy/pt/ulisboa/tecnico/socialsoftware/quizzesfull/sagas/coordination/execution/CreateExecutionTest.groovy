package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas.CreateExecutionFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateExecutionTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    def courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "createExecution: success"() {
        // Spec: plan.md §4 Execution — CreateExecution; orchestration outcome only, persistence in ExecutionServiceTest.
        when:
        def result = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        then:
        result.aggregateId != null
        result.acronym == ACRONYM_1
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createExecution: getCourseStep acquires READ_COURSE semantic lock"() {
        given: 'workflow pauses after getCourseStep'
        def uow1 = unitOfWorkService.createUnitOfWork("createExecution")
        def func1 = new CreateExecutionFunctionalitySagas(
                unitOfWorkService, ACRONYM_1, ACADEMIC_TERM_1, courseDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getCourseStep", uow1)

        expect: 'course is locked'
        sagaStateOf(courseDto.aggregateId) == CourseSagaState.READ_COURSE

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()
        func1.getCreatedExecutionDto().aggregateId != null
    }

}
