package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class BehavioralCoverageTest {

    @Test
    void measuresUniqueDuplicateAndCumulativeDiscoveryInRunOrder() {
        TestResult first = result("first", "second");
        TestResult duplicate = result("first", "second");
        TestResult novel = result("first", "third");

        BehavioralCoverage coverage = BehavioralCoverage.from(List.of(first, duplicate, novel));

        assertEquals(BehavioralFingerprint.SCHEMA, coverage.fingerprintSchema());
        assertEquals(3, coverage.runs());
        assertEquals(2, coverage.uniqueBehaviors());
        assertEquals(1, coverage.duplicateRuns());
        assertEquals(2.0 / 3.0, coverage.discoveryRate());
        assertEquals(List.of(1, 1, 2), coverage.cumulativeUniqueBehaviors());
        assertEquals(3, coverage.uniqueFeatures());
        assertEquals(2, coverage.runsAddingFeatures());
        assertEquals(2.0 / 3.0, coverage.featureDiscoveryRate());
        assertEquals(List.of(2, 2, 3), coverage.cumulativeUniqueFeatures());
        assertEquals(List.of(2, 0, 1), coverage.newFeaturesPerRun());
    }

    private static TestResult result(String firstFunctionality, String secondFunctionality) {
        StepId first = StepId.forFunctionalityStep(
                FunctionalityId.forSagaFunctionality(firstFunctionality), "step");
        StepId second = StepId.forFunctionalityStep(
                FunctionalityId.forSagaFunctionality(secondFunctionality), "step");
        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(first, second),
                Map.of(), Set.of(), List.of(), Set.of(), List.of(), List.of(), Map.of());
    }
}
