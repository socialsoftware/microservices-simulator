package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto

@DataJpaTest
@org.springframework.transaction.annotation.Transactional
@Import(LocalBeanConfiguration)
class GetQuizByIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def courseDto
    def executionDto
    def questionDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, "SE-2025", "2025/2026")
        questionDto = createQuestion(courseDto.aggregateId, [], "Quiz Question", "Describe algorithms.")
    }

    def "getQuizById: success"() {
        given:
        QuizDto created = createQuiz(executionDto.aggregateId, [questionDto.aggregateId])

        when:
        QuizDto result = quizFunctionalities.getQuizById(created.aggregateId)

        then:
        result != null
        result.aggregateId == created.aggregateId
        result.title == "Test Quiz"
        result.executionId == executionDto.aggregateId
        result.questionIds.contains(questionDto.aggregateId)
    }

    def "getQuizById: not found throws exception"() {
        when:
        quizFunctionalities.getQuizById(999999)

        then:
        thrown(SimulatorException)
    }
}
