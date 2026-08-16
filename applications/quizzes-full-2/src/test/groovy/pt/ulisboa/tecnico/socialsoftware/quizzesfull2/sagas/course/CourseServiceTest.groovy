package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto
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
        flushAndClear()
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
        flushAndClear()
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

    def "createCourse: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §1 Course — CreateCourse(name, type) postconditions
        given:
        def courseDto = new CourseDto()
        courseDto.setName("Distributed Systems")
        courseDto.setType(CourseType.EXTERNAL)

        when:
        def created = courseService.createCourse(courseDto,
                unitOfWorkService.createUnitOfWork("createCourse"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        created.aggregateId != null
        flushAndClear()
        def readBack = courseService.getCourseById(created.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.aggregateId == created.aggregateId
        readBack.name == "Distributed Systems"
        readBack.type == CourseType.EXTERNAL
        readBack.version != null
    }

    def "createCourse: each course gets its own aggregate id and state"() {
        // Spec: plan.md §1 Course — CreateCourse(name, type); courses are immutable and independent
        given:
        def firstDto = new CourseDto()
        firstDto.setName(COURSE_NAME)
        firstDto.setType(COURSE_TYPE)
        def secondDto = new CourseDto()
        secondDto.setName("Distributed Systems")
        secondDto.setType(CourseType.EXTERNAL)

        when:
        def first = courseService.createCourse(firstDto,
                unitOfWorkService.createUnitOfWork("createCourse"))
        def second = courseService.createCourse(secondDto,
                unitOfWorkService.createUnitOfWork("createCourse"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        first.aggregateId != second.aggregateId
        flushAndClear()
        def readBackFirst = courseService.getCourseById(first.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBackFirst.name == COURSE_NAME
        readBackFirst.type == COURSE_TYPE
        def readBackSecond = courseService.getCourseById(second.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBackSecond.name == "Distributed Systems"
        readBackSecond.type == CourseType.EXTERNAL
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
