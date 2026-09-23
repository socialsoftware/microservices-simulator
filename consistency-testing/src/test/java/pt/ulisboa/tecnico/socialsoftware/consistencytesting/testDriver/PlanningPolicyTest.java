package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlanningPolicyTest {

    @Test
    void parsesDocumentedPropertyValues() {
        assertEquals(PlanningPolicy.FOOTPRINT_CONFLICTS, PlanningPolicy.parse("footprint-conflicts"));
        assertEquals(PlanningPolicy.ALL_GROUPS, PlanningPolicy.parse(" ALL-GROUPS "));
    }

    @Test
    void rejectsUnknownPolicy() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> PlanningPolicy.parse("everything"));

        assertEquals(
                "Unknown planning policy 'everything'; expected one of [footprint-conflicts, all-groups]",
                error.getMessage());
    }
}
