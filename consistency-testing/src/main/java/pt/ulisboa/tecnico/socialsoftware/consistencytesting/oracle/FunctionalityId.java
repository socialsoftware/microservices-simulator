package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

public final class FunctionalityId {
    private static final String ID_CONNECTOR = "-";

    private final String id;

    private FunctionalityId(String id) {
        this.id = id;
    }

    public static FunctionalityId forSagaFunctionality(String functionalityId) {
        return new FunctionalityId(functionalityId);
    }

    public static FunctionalityId forEventHandlerFunctionality(
            Class<? extends Event> eventClazz,
            Class<? extends EventHandler> eventHandlerClazz,
            StepId emittingStepId,
            Integer subscriberAggregateId,
            Integer publisherAggregateId) {

        return new FunctionalityId(String.join(ID_CONNECTOR,
                "event", eventClazz.getName(),
                "fromAggregate", publisherAggregateId.toString(),
                "toAggregate", subscriberAggregateId.toString(),
                "emittedBy", emittingStepId.toString(),
                "withHandler", eventHandlerClazz.getName()));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionalityId other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}