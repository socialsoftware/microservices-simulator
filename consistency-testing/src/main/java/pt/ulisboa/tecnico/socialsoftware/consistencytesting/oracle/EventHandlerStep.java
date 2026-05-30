package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

public class EventHandlerStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final Set<StepId> dependencies;
    private final Event event;
    private final EventHandler eventHandler;
    private final Integer subscriberAggregateId;

    EventHandlerStep(
            Event event,
            EventHandler eventHandler,
            StepId emittingStepId,
            Integer publisherAggregateId,
            Integer subscriberAggregateId) {

        functionalityId = FunctionalityId.forEventHandlerFunctionality(
                event.getClass(),
                eventHandler.getClass(),
                emittingStepId,
                subscriberAggregateId,
                publisherAggregateId);

        id = StepId.forEventHandlerStep(functionalityId);
        this.event = event;
        this.eventHandler = eventHandler;
        this.subscriberAggregateId = subscriberAggregateId;
        dependencies = Set.of(emittingStepId);
    }

    @Override
    public void execute() {
        eventHandler.handleEvent(subscriberAggregateId, event);
    }

    @Override
    public StepId getId() {
        return id;
    }

    @Override
    public FunctionalityId getFunctionalityId() {
        return functionalityId;
    }

    @Override
    public Set<StepId> getDependencies() {
        return dependencies;
    }
}
