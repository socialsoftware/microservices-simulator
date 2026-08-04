package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate

import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter

/**
 * Directly exercises the persistence translation of saga states (SagaStateConverter),
 * without spinning up a real DB. Checks the round-trip both for the built-in
 * GenericSagaState and for a brand-new, test-local SagaState enum (TestSagaState).
 */
class SagaStatePersistenceTest extends SpockTest {

    // A new, test-only SagaState class living outside GenericSagaState, to confirm
    // the persistence layer handles arbitrary per-aggregate state enums.
    enum TestSagaState implements SagaAggregate.SagaState {
        TEST_FREE,
        TEST_LOCKED

        @Override
        String getStateName() {
            return name()
        }
    }

    private SagaStateConverter converter = new SagaStateConverter()

    def "GenericSagaState survives a db write then read round-trip"() {
        given: 'a state is written to its db column value'
        def column = converter.convertToDatabaseColumn(state)

        when: 'the column value is read back'
        def restored = converter.convertToEntityAttribute(column)

        then: 'the original state is recovered'
        restored == state

        where:
        state << [GenericSagaState.NOT_IN_SAGA, GenericSagaState.IN_SAGA]
    }

    def "a new TestSagaState survives a db write then read round-trip"() {
        given: 'a state is written to its db column value'
        def column = converter.convertToDatabaseColumn(state)

        when: 'the column value is read back'
        def restored = converter.convertToEntityAttribute(column)

        then: 'the original state is recovered'
        restored == state

        where:
        state << [TestSagaState.TEST_FREE, TestSagaState.TEST_LOCKED]
    }

    def "null state maps to null column and back"() {
        expect:
        converter.convertToDatabaseColumn(null) == null
        converter.convertToEntityAttribute(null) == null
    }
}
