package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.question

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
@TestPropertySource(locations = "classpath:application-test.properties")
class GetQuestionByIdTest extends QuizzesFullSpockTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def courseDto
    def executionDto
    def topicDto
    def questionDto

    def setup() {
        courseDto = createCourse("Software Engineering", "TECNICO")
        executionDto = createExecution(courseDto.aggregateId, "SE2024", "1st Semester 2024/2025")
        topicDto = createTopic(courseDto.aggregateId, "Algorithms")
        questionDto = createQuestion(courseDto.aggregateId, [topicDto.aggregateId], "What is a saga?", "Describe a saga pattern.")
    }

    def "getQuestionById: success"() {
        when:
        def result = questionFunctionalities.getQuestionById(questionDto.aggregateId)

        then:
        result.aggregateId == questionDto.aggregateId
        result.title == "What is a saga?"
        result.content == "Describe a saga pattern."
        result.courseAggregateId == courseDto.aggregateId
    }

    def "getQuestionById: aggregate not found"() {
        when:
        questionFunctionalities.getQuestionById(999)

        then:
        thrown(SimulatorException)
    }
}
