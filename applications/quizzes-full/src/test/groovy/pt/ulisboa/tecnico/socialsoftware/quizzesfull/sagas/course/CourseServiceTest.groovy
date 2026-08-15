package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CourseServiceTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "createCourse: persisted and readable through a fresh UnitOfWork (TECNICO)"() {
        // Spec: plan.md §1 Course — CreateCourse postconditions
        when:
        def dto = courseService.createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO,
                unitOfWorkService.createUnitOfWork("createCourse"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = courseService.getCourseById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == COURSE_NAME_1
        readBack.type == COURSE_TYPE_TECNICO
    }

    def "createCourse: persisted and readable through a fresh UnitOfWork (EXTERNAL)"() {
        // Spec: plan.md §1 Course — CreateCourse postconditions
        when:
        def dto = courseService.createCourse(COURSE_NAME_2, COURSE_TYPE_EXTERNAL,
                unitOfWorkService.createUnitOfWork("createCourse"))

        then: 'read back through a second, fresh UnitOfWork'
        def readBack = courseService.getCourseById(dto.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.name == COURSE_NAME_2
        readBack.type == COURSE_TYPE_EXTERNAL
    }

    def "getCourseById: not found throws SimulatorException"() {
        // Spec: plan.md §1 Course — GetCourseById not-found path (Path A: aggregateLoadAndRegisterRead)
        when:
        courseService.getCourseById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("getCourseById"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }

    def "updateCourse: COURSE_FIELDS_IMMUTABLE — name and type are P1 final fields"() {
        // Spec: plan.md §1 Course — UpdateCourse always rejected; name/type are final P1 fields
        given:
        def existing = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        when:
        courseService.updateCourse(existing.aggregateId, "New Name", COURSE_TYPE_EXTERNAL,
                unitOfWorkService.createUnitOfWork("updateCourse"))

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QuizzesFullErrorMessage.COURSE_FIELDS_IMMUTABLE
    }

    def "deleteCourse: removes course, not found via fresh UnitOfWork"() {
        // Spec: plan.md §1 Course — DeleteCourse postconditions (soft-delete)
        given:
        def existing = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        when:
        courseService.deleteCourse(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("deleteCourse"))

        and: 'verify course is no longer retrievable'
        courseService.getCourseById(existing.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }
}
