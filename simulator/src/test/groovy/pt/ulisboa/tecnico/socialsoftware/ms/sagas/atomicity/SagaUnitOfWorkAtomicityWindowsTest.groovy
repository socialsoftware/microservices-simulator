package pt.ulisboa.tecnico.socialsoftware.ms.sagas.atomicity


import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.command.AbortSagaCommand

import java.util.concurrent.atomic.AtomicInteger

class SagaUnitOfWorkAtomicityWindowsTest /*extends SpockTest*/ {

    def 'saga abort must still compensate when command dispatch fails'() {
        given:
        def service = new SagaUnitOfWorkService()
        def commandGateway = Mock(CommandGateway)
        setField(service, 'commandGateway', commandGateway)

        def unitOfWork = new SagaUnitOfWork(1L, 'ABORT_WINDOW')
        unitOfWork.addToAggregatesInSaga(7, 'ExecutionSaga')
        unitOfWork.savePreviousState(7, GenericSagaState.IN_SAGA)

        def compensationCalls = new AtomicInteger(0)
        unitOfWork.registerCompensation({ compensationCalls.incrementAndGet() })

        when:
        service.abort(unitOfWork)

        then:
        thrown(RuntimeException)
        compensationCalls.get() == 1
        1 * commandGateway.send({
            it instanceof AbortSagaCommand &&
                    it.aggregateId == 7 &&
                    it.serviceName == 'execution'
        }) >> { throw new RuntimeException('dispatch failure during abort') }
        0 * commandGateway.send(_)
    }

    private static void setField(Object target, String fieldName, Object value) {
        def field = target.class.getDeclaredField(fieldName)
        field.accessible = true
        field.set(target, value)
    }
}
