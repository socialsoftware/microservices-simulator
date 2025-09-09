package pt.ulisboa.tecnico.socialsoftware.answers

import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuizAnswerDto
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class AnswersSpockTest extends SpockTest {
    @Autowired
    AnswerFunctionalities answerFunctionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create QuizAnswer"() {
        when:
        def answerDto = new QuizAnswerDto()
        answerFunctionalities.createAnswer(answerDto)
        then:
        noExceptionThrown()
    }
}
