package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect.EffectKind;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroup;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.PlanningPolicy;

class OrchestratorPlanningPolicyTest {

    private static final String PROPERTY = "consistency.planningPolicy";
    private String originalValue;

    @BeforeEach
    void clearProperty() {
        originalValue = System.getProperty(PROPERTY);
        System.clearProperty(PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (originalValue == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, originalValue);
        }
    }

    @Test
    void defaultsToFootprintConflicts() {
        assertEquals(
                PlanningPolicy.FOOTPRINT_CONFLICTS,
                Orchestrator.of(getClass()).getPlanningPolicy());
    }

    @Test
    void readsAllGroupsFromSystemProperty() {
        System.setProperty(PROPERTY, "all-groups");

        assertEquals(PlanningPolicy.ALL_GROUPS, Orchestrator.of(getClass()).getPlanningPolicy());
    }

    @Test
    void rejectsUnknownSystemPropertyValue() {
        System.setProperty(PROPERTY, "unknown");

        assertThrows(IllegalArgumentException.class, () -> Orchestrator.of(getClass()));
    }

    @Test
    void planHashIsStableButIncludesPolicyAndConflictEvidence() {
        Orchestrator.PlannedCatalog plan = plan(group(Set.of()));
        String first = Orchestrator.planHashOf(PlanningPolicy.ALL_GROUPS, List.of(plan));

        assertEquals(first, Orchestrator.planHashOf(PlanningPolicy.ALL_GROUPS, List.of(plan)));
        assertEquals(hashOfCanonicalPlan("catalog|2|3\nfirst__second|first|second|"),
                Orchestrator.planHashOf(PlanningPolicy.FOOTPRINT_CONFLICTS, List.of(plan)));
        assertNotEquals(
                first,
                Orchestrator.planHashOf(PlanningPolicy.FOOTPRINT_CONFLICTS, List.of(plan)));
        assertNotEquals(
                first,
                Orchestrator.planHashOf(PlanningPolicy.ALL_GROUPS, List.of(plan(group(Set.of(
                        new FunctionalityGroup.Conflict(
                                "aggregate", Set.of(EffectKind.READ), Set.of(EffectKind.WRITE))))))));
    }

    private static String hashOfCanonicalPlan(String canonicalPlan) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonicalPlan.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }

    private static Orchestrator.PlannedCatalog plan(FunctionalityGroup group) {
        return new Orchestrator.PlannedCatalog(
                new FunctionalityCatalog("catalog", () -> null, Map.of()),
                2, 3, List.of(group));
    }

    private static FunctionalityGroup group(Set<FunctionalityGroup.Conflict> conflicts) {
        return new FunctionalityGroup(
                FunctionalityId.forSagaFunctionality("first"),
                FunctionalityId.forSagaFunctionality("second"),
                conflicts);
    }
}
