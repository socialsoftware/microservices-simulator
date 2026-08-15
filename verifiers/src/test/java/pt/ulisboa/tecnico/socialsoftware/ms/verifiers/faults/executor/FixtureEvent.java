package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

public class FixtureEvent extends Event {
    public FixtureEvent() {
    }

    public FixtureEvent(Integer publisherAggregateId) {
        super(publisherAggregateId);
    }
}
