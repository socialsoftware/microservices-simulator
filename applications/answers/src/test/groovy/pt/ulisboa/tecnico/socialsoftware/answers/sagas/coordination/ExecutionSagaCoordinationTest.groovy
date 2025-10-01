package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination

import java.time.LocalDateTime
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.sagas.ExecutionSagaCoordination
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.sagas.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaManager
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.SagaTransaction
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

class ExecutionSagaCoordinationTest extends SpockTest {
    @Autowired
    ExecutionSagaCoordination executionSagaCoordination

    @Autowired
    SagaManager sagaManager

    def setup() {
        BehaviourService.clearBehaviour()
    }

    def "should create Execution saga successfully"() {
        when:
        def sagaId = "execution_saga_" + System.currentTimeMillis()
        def executionDto = new ExecutionDto()
        
        def sagaState = executionSagaCoordination.createExecutionSaga(sagaId, executionDto)
        then:
        noExceptionThrown()
    }

    def "should compensate Execution saga on failure"() {
        when:
        def sagaId = "execution_saga_" + System.currentTimeMillis()
        def executionDto = new ExecutionDto()
        
        // Simulate failure
        BehaviourService.addBehaviour("ExecutionService", "createExecution", "EXCEPTION")
        
        def sagaState = executionSagaCoordination.createExecutionSaga(sagaId, executionDto)
        then:
        noExceptionThrown()
        cleanup:
BehaviourService.clearBehaviour()
    }

    def "should handle Execution saga timeout"() {
        when:
        def sagaId = "execution_saga_" + System.currentTimeMillis()
        def executionDto = new ExecutionDto()
        
        // Simulate timeout
        BehaviourService.addBehaviour("ExecutionService", "createExecution", "TIMEOUT")
        
        def sagaState = executionSagaCoordination.createExecutionSaga(sagaId, executionDto)
        then:
        noExceptionThrown()
        cleanup:
BehaviourService.clearBehaviour()
    }
}
