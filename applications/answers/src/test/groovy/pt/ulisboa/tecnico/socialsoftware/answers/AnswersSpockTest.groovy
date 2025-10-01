package pt.ulisboa.tecnico.socialsoftware.answers

import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerDto
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class AnswersSpockTest extends SpockTest {
    @Autowired
    AnswerFunctionalities answerFunctionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create Answer"() {
        when:
        def answerDto = new AnswerDto()
        answerFunctionalities.createAnswer(answerDto)
        then:
        noExceptionThrown()
    }
}
