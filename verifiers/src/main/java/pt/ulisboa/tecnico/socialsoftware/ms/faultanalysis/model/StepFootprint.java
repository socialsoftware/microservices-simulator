package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import org.jspecify.annotations.NonNull;

import java.util.*;

public record StepFootprint(SagaStepBuildingBlock step, String aggregateName, AccessPolicy accessPolicy) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StepFootprint that)) return false;
        return Objects.equals(step, that.step)
                && Objects.equals(aggregateName, that.aggregateName)
                && accessPolicy == that.accessPolicy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(step, aggregateName, accessPolicy);
    }

    @Override
    public @NonNull String toString() {
        return step.getName() + " - " + accessPolicy + "(" + aggregateName + ")";
    }
}
