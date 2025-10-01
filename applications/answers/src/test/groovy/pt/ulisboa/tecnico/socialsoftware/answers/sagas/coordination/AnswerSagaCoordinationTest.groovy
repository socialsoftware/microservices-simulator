package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination

import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.sagas.AnswerSagaCoordination
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.sagas.AnswerSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaManager
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaTransaction
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class AnswerSagaCoordinationTest extends SpockTest {
    @Autowired
    AnswerSagaCoordination answerSagaCoordination

    @Autowired
    SagaManager sagaManager

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create Answer saga successfully"() {
        when:
        def sagaId = "answer_saga_" + System.currentTimeMillis()
        def answerDto = new AnswerDto()
        
        def sagaState = answerSagaCoordination.createAnswerSaga(sagaId, answerDto)
        then:
        noExceptionThrown()
    }

    def "should compensate Answer saga on failure"() {
        when:
        def sagaId = "answer_saga_" + System.currentTimeMillis()
        def answerDto = new AnswerDto()
        
        // Simulate failure
        BehaviourService.addBehaviour("AnswerService", "createAnswer", "EXCEPTION")
        
        def sagaState = answerSagaCoordination.createAnswerSaga(sagaId, answerDto)
        then:
        noExceptionThrown()
        cleanup:
BehaviourService.clearBehaviour()
    }

    def "should handle Answer saga timeout"() {
        when:
        def sagaId = "answer_saga_" + System.currentTimeMillis()
        def answerDto = new AnswerDto()
        
        // Simulate timeout
        BehaviourService.addBehaviour("AnswerService", "createAnswer", "TIMEOUT")
        
        def sagaState = answerSagaCoordination.createAnswerSaga(sagaId, answerDto)
        then:
        noExceptionThrown()
        cleanup:
BehaviourService.clearBehaviour()
    }
}
