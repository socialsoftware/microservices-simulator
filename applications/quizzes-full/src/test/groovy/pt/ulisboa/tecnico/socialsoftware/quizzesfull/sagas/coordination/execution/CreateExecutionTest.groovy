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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.UpdateCourseFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
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

    public static final String ACRONYM_2 = "DS01"
    public static final String ACADEMIC_TERM_2 = "2nd Semester 2024/2025"

    def courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "createExecution: success"() {
        // Spec: Execution.{acronym,academicTerm,courseId} = input;
        //       SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.4 Execution / createExecution.
        when:
        def result = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        then:
        result.aggregateId != null
        result.acronym == ACRONYM_1
        result.academicTerm == ACADEMIC_TERM_1
        result.courseId == courseDto.aggregateId
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createExecution: NO_DUPLICATE_COURSE_EXECUTION violation"() {
        given:
        createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        when:
        createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        then:
        def ex = thrown(QuizzesFullException)
        ex.errorMessage == QuizzesFullErrorMessage.NO_DUPLICATE_COURSE_EXECUTION
    }

    def "createExecution: different acronym same academic term is allowed"() {
        given:
        createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        // Spec: Execution.{acronym,academicTerm,courseId} = input;
        //       SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.4 Execution / createExecution.
        when:
        def result = createExecution(courseDto.aggregateId, ACRONYM_2, ACADEMIC_TERM_1)

        then:
        result.aggregateId != null
        result.acronym == ACRONYM_2
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

    def "createExecution: incrementCourseExecutionCountStep sees forbidden state when course is locked by concurrent updateCourse"() {
        given:
        def uow1 = unitOfWorkService.createUnitOfWork("createExecution")
        def func1 = new CreateExecutionFunctionalitySagas(
                unitOfWorkService, ACRONYM_1, ACADEMIC_TERM_1, courseDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("createExecutionStep", uow1)

        and: 'concurrent updateCourse acquires IN_UPDATE_COURSE on the same course'
        def uow2 = unitOfWorkService.createUnitOfWork("updateCourse")
        def func2 = new UpdateCourseFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, COURSE_NAME_1, COURSE_TYPE_TECNICO, uow2, commandGateway)
        func2.executeUntilStep("getCourseStep", uow2)

        when: 'createExecution resumes into the forbidden course state'
        func1.resumeWorkflow(uow1)

        then:
        thrown(SimulatorException)
    }
}
