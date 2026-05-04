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
class GetQuestionsByCourseExecutionIdTest extends QuizzesFullSpockTest {

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

    def "getQuestionsByCourseExecutionId: success"() {
        when:
        def result = questionFunctionalities.getQuestionsByCourseExecutionId(executionDto.aggregateId)

        then:
        result.size() == 1
        result[0].aggregateId == questionDto.aggregateId
        result[0].title == "What is a saga?"
        result[0].courseAggregateId == courseDto.aggregateId
    }

    def "getQuestionsByCourseExecutionId: no questions returns empty list"() {
        given:
        def course2 = createCourse("Distributed Systems", "TECNICO")
        def execution2 = createExecution(course2.aggregateId, "DS2024", "1st Semester 2024/2025")

        when:
        def result = questionFunctionalities.getQuestionsByCourseExecutionId(execution2.aggregateId)

        then:
        result.isEmpty()
    }

    def "getQuestionsByCourseExecutionId: execution not found"() {
        when:
        questionFunctionalities.getQuestionsByCourseExecutionId(999)

        then:
        thrown(SimulatorException)
    }
}
