package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.SagaCourse
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

@DataJpaTest
@org.springframework.transaction.annotation.Transactional
@Import(LocalBeanConfiguration)
class CourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create course"() {
        when:
        def course = new SagaCourse(1, "Software Engineering", "TECNICO")

        then:
        course.name == "Software Engineering"
        course.type == CourseType.TECNICO
        course.executionCount == 0
        course.questionCount == 0
    }

    def "create course — CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT"() {
        given:
        def course = new SagaCourse(1, "Software Engineering", "TECNICO")

        when:
        course.setQuestionCount(1)
        course.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QuizzesFullErrorMessage.CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
    }
}
