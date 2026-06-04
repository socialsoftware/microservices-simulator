package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.SagaCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CourseIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create course"() {
        when:
        def courseDto = courseFunctionalities.createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        then:
        courseDto.name == COURSE_NAME_1
        courseDto.type == COURSE_TYPE_TECNICO
        courseDto.executionCount == 0
        courseDto.questionCount == 0
    }

    // -----------------------------------------------------------------------
    // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    // Predicate: executionCount == 0 ⟹ questionCount == 0
    // BVA straddle:
    //   on-point  (executionCount=0, questionCount=0) — just satisfies, no throw
    //   off-point (executionCount=0, questionCount=1) — just violates, throws
    // -----------------------------------------------------------------------

    def "CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT: on-point — executionCount=0 and questionCount=0 passes"() {
        given: 'a course with no executions and no questions (initial state)'
        def course = new SagaCourse(1, COURSE_NAME_1, COURSE_TYPE_TECNICO)
        course.setExecutionCount(0)
        course.setQuestionCount(0)

        when:
        course.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT: off-point — executionCount=0 and questionCount=1 violates invariant"() {
        given: 'a course with no executions but one question (impossible via service, but direct state manipulation reaches it)'
        def course = new SagaCourse(2, COURSE_NAME_1, COURSE_TYPE_TECNICO)
        course.setExecutionCount(0)
        course.setQuestionCount(1)

        when:
        course.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    }

    def "CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT: executionCount > 0 and questionCount > 0 passes"() {
        given: 'a course with one execution and one question'
        def course = new SagaCourse(3, COURSE_NAME_1, COURSE_TYPE_TECNICO)
        course.setExecutionCount(1)
        course.setQuestionCount(1)

        when:
        course.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }
}
