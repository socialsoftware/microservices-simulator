package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType

@DataJpaTest
@Transactional
@Import(CourseServiceTest.LocalBeanConfiguration)
class CourseServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999

    def "getCourseById: reads back the persisted course through a fresh UnitOfWork"() {
        // Spec: plan.md §1 Course — GetCourseById(courseAggregateId); fields name, type
        given:
        def courseAggregateId = createCourse()

        when:
        def result = courseService.getCourseById(courseAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == courseAggregateId
        result.name == COURSE_NAME
        result.type == COURSE_TYPE
        result.version != null
    }

    def "getCourseById: unknown aggregate id is not found"() {
        // Spec: plan.md §1 Course — GetCourseById(courseAggregateId); Path A (aggregateLoadAndRegisterRead)
        when:
        courseService.getCourseById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getCourses: returns every persisted course with its own state"() {
        // Spec: plan.md §1 Course — GetCourses()
        given:
        def firstAggregateId = createCourse(COURSE_NAME, COURSE_TYPE)
        def secondAggregateId = createCourse("Distributed Systems", CourseType.EXTERNAL)

        when:
        def result = courseService.getCourses(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.name == COURSE_NAME
        first.type == COURSE_TYPE
        def second = result.find { it.aggregateId == secondAggregateId }
        second.name == "Distributed Systems"
        second.type == CourseType.EXTERNAL
    }

    def "getCourses: returns an empty list when no course exists"() {
        // Spec: plan.md §1 Course — GetCourses()
        when:
        def result = courseService.getCourses(unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
