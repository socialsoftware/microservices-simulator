package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

final class EventHandlerStep implements OracleStep {

    private final StepId id;
    private final FunctionalityId functionalityId;
    private final Set<StepId> dependencies;
    private final Event event;
    private final EventHandler eventHandler;
    private final Integer eventId;
    private final Integer subscriberAggregateId;

    EventHandlerStep(DeferredEventInvocation invocation, StepId capturedAfterStepId) {
        Objects.requireNonNull(invocation, "Deferred event invocation cannot be null");
        this.event = invocation.event();
        this.eventHandler = invocation.handler();
        this.eventId = invocation.eventId();
        this.subscriberAggregateId = invocation.subscriberAggregateId();
        this.functionalityId = FunctionalityId.forEventHandlerFunctionality(invocation, capturedAfterStepId);
        this.id = StepId.forEventHandlerStep(this.functionalityId);
        this.dependencies = Set.of(Objects.requireNonNull(
                capturedAfterStepId, "Captured-after step ID cannot be null"));
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

    Integer eventId() {
        return eventId;
    }

    @Override
    public Set<StepId> getDependencies() {
        return dependencies;
    }
}
