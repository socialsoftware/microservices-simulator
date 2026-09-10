package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork

import jakarta.persistence.EntityManager
import org.springframework.test.util.ReflectionTestUtils
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregateRepository
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService

class SagaUnitOfWorkServiceSemanticLockTest extends SpockTest {

    def "semantic-lock acquisition rejects an aggregate already in a saga"() {
        given: 'an aggregate whose lock is held by another saga'
        def repository = Mock(SagaAggregateRepository)
        def entityManager = Mock(EntityManager)
        def aggregate = new TestSagaAggregate(TestSagaState.FIRST_SAGA)
        repository.findNonDeletedSagaAggregate(1) >> Optional.of(aggregate)
        def service = serviceWith(repository, entityManager)

        when: 'a second saga tries to acquire a semantic lock'
        service.registerSagaState(1, TestSagaState.SECOND_SAGA, new SagaUnitOfWork(2L, 'second operation'))

        then: 'the existing lock is preserved and acquisition is rejected'
        def exception = thrown(SimulatorException)
        exception.message == 'Aggregate is being used in FIRST_SAGA saga.'
        aggregate.sagaState == TestSagaState.FIRST_SAGA
        0 * entityManager.merge(_)
    }

    def "semantic-lock acquisition accepts an aggregate outside a saga"() {
        given: 'an aggregate without a semantic lock'
        def repository = Mock(SagaAggregateRepository)
        def entityManager = Mock(EntityManager)
        def aggregate = new TestSagaAggregate(GenericSagaState.NOT_IN_SAGA)
        repository.findNonDeletedSagaAggregate(1) >> Optional.of(aggregate)
        def service = serviceWith(repository, entityManager)
        def unitOfWork = new SagaUnitOfWork(1L, 'first operation')

        when: 'a saga acquires its semantic lock'
        service.registerSagaState(1, TestSagaState.SECOND_SAGA, unitOfWork)

        then: 'the requested lock is persisted and recorded for lifecycle cleanup'
        aggregate.sagaState == TestSagaState.SECOND_SAGA
        unitOfWork.aggregatesInSaga[1] == 'SagaTestAggregate'
        1 * entityManager.merge(aggregate)
    }

    private static SagaUnitOfWorkService serviceWith(SagaAggregateRepository repository, EntityManager entityManager) {
        def service = new SagaUnitOfWorkService()
        ReflectionTestUtils.setField(service, 'sagaAggregateRepository', repository)
        ReflectionTestUtils.setField(service, 'entityManager', entityManager)
        service
    }

    private static class TestSagaAggregate extends Aggregate implements SagaAggregate {
        private SagaState sagaState

        TestSagaAggregate(SagaState sagaState) {
            this.sagaState = sagaState
            aggregateType = 'SagaTestAggregate'
        }

        @Override
        void setSagaState(SagaState sagaState) {
            this.sagaState = sagaState
        }

        @Override
        SagaState getSagaState() {
            sagaState
        }

        @Override
        void verifyInvariants() {
        }

        @Override
        Set<EventSubscription> getEventSubscriptions() {
            Set.of()
        }
    }

    private enum TestSagaState implements SagaState {
        FIRST_SAGA,
        SECOND_SAGA

        @Override
        String getStateName() {
            name()
        }
    }
}
