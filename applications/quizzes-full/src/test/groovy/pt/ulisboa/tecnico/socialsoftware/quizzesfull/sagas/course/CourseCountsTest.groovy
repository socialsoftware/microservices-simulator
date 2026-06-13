package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CourseCountsTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    CourseDto courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "incrementExecutionCount: success"() {
        when:
        def uow = unitOfWorkService.createUnitOfWork("incrementExecutionCount")
        courseService.incrementExecutionCount(courseDto.aggregateId, uow)
        unitOfWorkService.commit(uow)

        then:
        def result = courseFunctionalities.getCourseById(courseDto.aggregateId)
        result.executionCount == 1
    }

    def "incrementQuestionCount: success"() {
        given: 'execution count is 1 so invariant allows questions'
        def uow0 = unitOfWorkService.createUnitOfWork("incrementExecutionCount")
        courseService.incrementExecutionCount(courseDto.aggregateId, uow0)
        unitOfWorkService.commit(uow0)

        when:
        def uow = unitOfWorkService.createUnitOfWork("incrementQuestionCount")
        courseService.incrementQuestionCount(courseDto.aggregateId, uow)
        unitOfWorkService.commit(uow)

        then:
        def result = courseFunctionalities.getCourseById(courseDto.aggregateId)
        result.questionCount == 1
    }

    def "decrementExecutionCount: success when questionCount is zero"() {
        given: 'execution count is 1'
        def uow1 = unitOfWorkService.createUnitOfWork("incrementExecutionCount")
        courseService.incrementExecutionCount(courseDto.aggregateId, uow1)
        unitOfWorkService.commit(uow1)

        when: 'decrement brings count to 0 while questionCount remains 0'
        def uow2 = unitOfWorkService.createUnitOfWork("decrementExecutionCount")
        courseService.decrementExecutionCount(courseDto.aggregateId, uow2)
        unitOfWorkService.commit(uow2)

        then:
        def result = courseFunctionalities.getCourseById(courseDto.aggregateId)
        result.executionCount == 0
    }

    def "decrementExecutionCount: floor at zero"() {
        when: 'decrement on a course with executionCount already 0'
        def uow = unitOfWorkService.createUnitOfWork("decrementExecutionCount")
        courseService.decrementExecutionCount(courseDto.aggregateId, uow)
        unitOfWorkService.commit(uow)

        then:
        def result = courseFunctionalities.getCourseById(courseDto.aggregateId)
        result.executionCount == 0
    }

    def "decrementQuestionCount: success"() {
        given: 'execution count = 1 and question count = 1'
        def uow0 = unitOfWorkService.createUnitOfWork("incrementExecutionCount")
        courseService.incrementExecutionCount(courseDto.aggregateId, uow0)
        unitOfWorkService.commit(uow0)

        def uow1 = unitOfWorkService.createUnitOfWork("incrementQuestionCount")
        courseService.incrementQuestionCount(courseDto.aggregateId, uow1)
        unitOfWorkService.commit(uow1)

        when:
        def uow2 = unitOfWorkService.createUnitOfWork("decrementQuestionCount")
        courseService.decrementQuestionCount(courseDto.aggregateId, uow2)
        unitOfWorkService.commit(uow2)

        then:
        def result = courseFunctionalities.getCourseById(courseDto.aggregateId)
        result.questionCount == 0
    }

    def "decrementQuestionCount: floor at zero"() {
        when: 'decrement on a course with questionCount already 0'
        def uow = unitOfWorkService.createUnitOfWork("decrementQuestionCount")
        courseService.decrementQuestionCount(courseDto.aggregateId, uow)
        unitOfWorkService.commit(uow)

        then:
        def result = courseFunctionalities.getCourseById(courseDto.aggregateId)
        result.questionCount == 0
    }
}
