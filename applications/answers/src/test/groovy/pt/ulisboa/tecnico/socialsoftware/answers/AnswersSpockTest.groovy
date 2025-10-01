package pt.ulisboa.tecnico.socialsoftware.answers

import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class AnswersSpockTest extends SpockTest {
    @Autowired
    ExecutionFunctionalities executionFunctionalities

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create Execution"() {
        when:
        def executionDto = new ExecutionDto()
        executionFunctionalities.createExecution(executionDto)
        then:
        noExceptionThrown()
    }
}
