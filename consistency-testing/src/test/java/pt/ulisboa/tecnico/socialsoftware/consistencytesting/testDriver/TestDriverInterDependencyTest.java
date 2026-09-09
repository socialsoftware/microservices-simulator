package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
class TestDriverInterDependencyTest {

    @Test
    void concreteEventDeliveryIsNotReusedAsACrossRunDependencyCandidate() {
        StepId firstFunctionalityStep = functionalityStep("first", "write");
        StepId secondFunctionalityStep = functionalityStep("second", "read");
        StepId concreteEventStep = eventHandlerStep();

        assertFalse(concreteEventStep.isIdentityStableAcrossRuns());
        assertTrue(firstFunctionalityStep.isIdentityStableAcrossRuns());
        assertTrue(secondFunctionalityStep.isIdentityStableAcrossRuns());

        Set<TestDriver.InterDependency> candidates = new HashSet<>(TestDriver.crossFunctionalityPairs(
                Set.of(firstFunctionalityStep, secondFunctionalityStep, concreteEventStep)));

        assertEquals(
            Set.of(
                new TestDriver.InterDependency(firstFunctionalityStep, secondFunctionalityStep),
                new TestDriver.InterDependency(secondFunctionalityStep, firstFunctionalityStep)),
            candidates);
    }

    @Test
    void ordinaryFunctionalityStepsRemainCrossRunDependencyCandidates() {
        StepId first = functionalityStep("first", "write");
        StepId second = functionalityStep("second", "read");

        assertEquals(
            Set.of(
                new TestDriver.InterDependency(first, second),
                new TestDriver.InterDependency(second, first)),
            new HashSet<>(TestDriver.crossFunctionalityPairs(Set.of(first, second))));
    }

    private static StepId functionalityStep(String functionality, String step) {
        return StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality(functionality), step);
    }

    private static StepId eventHandlerStep() {
        return StepId.forEventHandlerStep(FunctionalityId.forSagaFunctionality("event-handler"));
    }
}
