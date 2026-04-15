package pt.ulisboa.tecnico.socialsoftware.ms.sagas.atomicity

import jakarta.persistence.EntityManager
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregateRepository
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.command.CommitSagaCommand

class SagaCommitLockReleaseFailureTest extends SpockTest {

    def 'commit failure while releasing lock keeps aggregate locked for next functionality'() {
        given:
        def service = new SagaUnitOfWorkService()
        def commandGateway = Mock(CommandGateway)

        def lockedAggregate = new TestSagaAggregate(42, 'ExecutionSaga', GenericSagaState.NOT_IN_SAGA)

        def repository = Stub(SagaAggregateRepository) {
            findNonDeletedSagaAggregate(42) >> Optional.of(lockedAggregate)
        }
        def entityManager = Stub(EntityManager) {
            merge(_ as Object) >> { args -> args[0] }
        }

        setField(service, 'commandGateway', commandGateway)
        setField(service, 'sagaAggregateRepository', repository)
        setField(service, 'entityManager', entityManager)

        def firstFunctionalityUnitOfWork = new SagaUnitOfWork(100L, 'firstFunctionality')
        def secondFunctionality = new SimpleUseAggregateFunctionality(service)

        when: 'first functionality acquires semantic lock on the aggregate'
        service.registerSagaState(42, GenericSagaState.IN_SAGA, firstFunctionalityUnitOfWork)

        then:
        lockedAggregate.sagaState == GenericSagaState.IN_SAGA
        firstFunctionalityUnitOfWork.aggregatesInSaga == [42: 'ExecutionSaga']

        when: 'commit that should release the lock fails while dispatching CommitSagaCommand'
        service.commit(firstFunctionalityUnitOfWork)

        then:
        def commitFailure = thrown(RuntimeException)
        commitFailure.message == 'release lock dispatch failure'
        lockedAggregate.sagaState == GenericSagaState.IN_SAGA
        1 * commandGateway.send({
            it instanceof CommitSagaCommand &&
                    it.aggregateId == 42 &&
                    it.serviceName == 'execution'
        }) >> { throw new RuntimeException('release lock dispatch failure') }

        when: 'a new simple functionality tries to use the same aggregate'
        secondFunctionality.execute(42)

        then:
        def lockError = thrown(SimulatorException)
        lockError.message.contains('IN_SAGA')
    }

    private static void setField(Object target, String fieldName, Object value) {
        def field = target.class.getDeclaredField(fieldName)
        field.accessible = true
        field.set(target, value)
    }

    static class SimpleUseAggregateFunctionality {
        private final SagaUnitOfWorkService sagaUnitOfWorkService

        SimpleUseAggregateFunctionality(SagaUnitOfWorkService sagaUnitOfWorkService) {
            this.sagaUnitOfWorkService = sagaUnitOfWorkService
        }

        void execute(Integer aggregateId) {
            sagaUnitOfWorkService.verifySagaState(aggregateId, [GenericSagaState.IN_SAGA])
        }
    }

    static class TestSagaAggregate extends Aggregate implements SagaAggregate {
        private SagaState sagaState

        TestSagaAggregate(Integer aggregateId, String aggregateType, SagaState initialState) {
            super(aggregateId)
            setAggregateType(aggregateType)
            this.sagaState = initialState
        }

        @Override
        void setSagaState(SagaState state) {
            this.sagaState = state
        }

        @Override
        SagaState getSagaState() {
            return this.sagaState
        }

        @Override
        void verifyInvariants() {
            // No-op: this test only validates lock lifecycle behavior.
        }

        @Override
        Set<EventSubscription> getEventSubscriptions() {
            return [] as Set
        }
    }
}