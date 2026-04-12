package com.example.dummyapp

import spock.lang.Specification

// Sample saga state enum for testing
enum DummySagaState implements pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState {
    STARTED("STARTED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String stateName
    DummySagaState(String stateName) { this.stateName = stateName }
    String getStateName() { stateName }
}

class DummyAppSpec extends Specification {

    // Field initialization: direct inline saga construction
    def sagaInField = new DummySagaAggregate(100)

    def setup() {
        // setup: direct inline saga construction
        sagaInSetup = new DummySagaAggregate(200)
        sagaInSetup.setSagaState(DummySagaState.STARTED)
    }

    def setupSpec() {
        // setupSpec: direct inline saga construction
        sagaInSetupSpec = new DummySagaAggregate(300)
        sagaInSetupSpec.setSagaState(DummySagaState.COMPLETED)
    }

    // Instance fields set by setup
    def sagaInSetup

    // Static field set by setupSpec
    static sagaInSetupSpec

    def 'verify saga in field initializer'() {
        expect:
        sagaInField.aggregateId == 100
    }

    def 'verify saga constructed in setup'() {
        expect:
        sagaInSetup.aggregateId == 200
    }

    def 'verify saga constructed in setupSpec'() {
        expect:
        sagaInSetupSpec.aggregateId == 300
    }

    // Near-miss: plain aggregate construction WITHOUT saga interface
    def 'non-saga aggregate construction (near miss)'() {
        when:
        def plainAgg = new DummyAggregate(999, 'plain')

        then:
        plainAgg.aggregateId == 999
    }
}

// Dummy saga aggregate implementation for testing
class DummySagaAggregate extends DummyAggregate implements pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate {
    
    private pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState sagaState

    DummySagaAggregate(Integer aggregateId) {
        super(aggregateId, "saga-" + aggregateId)
    }

    @Override
    void setSagaState(pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState state) {
        this.sagaState = state
    }

    @Override
    pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregate.SagaState getSagaState() {
        return sagaState
    }
}
