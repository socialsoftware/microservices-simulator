package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroup;

class OrchestratorGroupSelectionTest {

    private static final String GROUP_SELECTORS_PROPERTY = "consistency.groupSelectors";

    private String originalGroupSelectors;

    @BeforeEach
    void clearGroupSelectorsProperty() {
        originalGroupSelectors = System.getProperty(GROUP_SELECTORS_PROPERTY);
        System.clearProperty(GROUP_SELECTORS_PROPERTY);
    }

    @AfterEach
    void restoreGroupSelectorsProperty() {
        if (originalGroupSelectors == null) {
            System.clearProperty(GROUP_SELECTORS_PROPERTY);
        } else {
            System.setProperty(GROUP_SELECTORS_PROPERTY, originalGroupSelectors);
        }
    }

    @Test
    void readsCanonicalExactSelectorsFromSystemProperty() {
        System.setProperty(
                GROUP_SELECTORS_PROPERTY,
                " second-catalog/third__fourth , first-catalog/first__second ");

        assertEquals(
                List.of("first-catalog/first__second", "second-catalog/third__fourth"),
                Orchestrator.of(getClass()).configuredGroupSelectors());
    }

    @Test
    void rejectsMalformedSelectorInsteadOfSilentlyRunningEverything() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Orchestrator.of(getClass()).withGroupSelectors(List.of("catalog-only")));

        assertEquals(
                "Group selector must have form '<catalog>/<group-label>', got: 'catalog-only'",
                error.getMessage());
    }

    @Test
    void fluentConfigurationReturnsSameOrchestrator() {
        Orchestrator orchestrator = Orchestrator.of(getClass());

        assertSame(orchestrator, orchestrator.withGroupSelectors(List.of("catalog/first__second")));
    }

    @Test
    void selectsCatalogsBeforeProfiling() {
        FunctionalityCatalog first = catalog("first-catalog");
        FunctionalityCatalog second = catalog("second-catalog");

        List<FunctionalityCatalog> selected = Orchestrator.selectCatalogs(
                List.of(first, second),
                Set.of(GroupSelector.parse("second-catalog/third__fourth")));

        assertEquals(List.of("second-catalog"), selected.stream().map(FunctionalityCatalog::name).toList());
    }

    @Test
    void emptySelectionPreservesWholePlan() {
        List<FunctionalityCatalog> catalogs = List.of(catalog("first-catalog"), catalog("second-catalog"));
        List<Orchestrator.PlannedCatalog> plans = List.of(
                plan(catalogs.get(0), group("first", "second")),
                plan(catalogs.get(1), group("third", "fourth")));

        assertEquals(catalogs, Orchestrator.selectCatalogs(catalogs, Set.of()));
        assertEquals(plans, Orchestrator.selectPlannedGroups(plans, Set.of()));
    }

    @Test
    void rejectsUnknownCatalogBeforeProfiling() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> Orchestrator.selectCatalogs(
                        List.of(catalog("catalog")),
                        Set.of(GroupSelector.parse("missing/first__second"))));

        assertEquals("Unknown catalog(s) in group selectors: [missing]", error.getMessage());
    }

    @Test
    void keepsOnlyExactGroupsInEffectivePlan() {
        FunctionalityCatalog firstCatalog = catalog("first-catalog");
        FunctionalityCatalog secondCatalog = catalog("second-catalog");
        FunctionalityGroup firstSelected = group("first", "second");
        FunctionalityGroup firstSkipped = group("first", "third");
        FunctionalityGroup secondSelected = group("third", "fourth");
        List<Orchestrator.PlannedCatalog> plans = List.of(
                plan(firstCatalog, firstSelected, firstSkipped),
                plan(secondCatalog, secondSelected));

        List<Orchestrator.PlannedCatalog> selected = Orchestrator.selectPlannedGroups(
                plans,
                Set.of(
                        GroupSelector.parse("first-catalog/first__second"),
                        GroupSelector.parse("second-catalog/fourth__third")));

        assertEquals(List.of("first-catalog", "second-catalog"), selected.stream()
                .map(plan -> plan.catalog().name())
                .toList());
        assertEquals(List.of("first__second"), selected.get(0).groups().stream()
                .map(FunctionalityGroup::label)
                .toList());
        assertEquals(List.of("fourth__third"), selected.get(1).groups().stream()
                .map(FunctionalityGroup::label)
                .toList());
    }

    @Test
    void rejectsSelectorThatDoesNotMatchAPlannedGroup() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
            () -> Orchestrator.selectPlannedGroups(
                        List.of(plan(catalog("catalog"), group("first", "second"))),
                        Set.of(GroupSelector.parse("catalog/missing__group"))));

        assertEquals("No planned group matched selector(s): [catalog/missing__group]", error.getMessage());
    }

    private static FunctionalityCatalog catalog(String name) {
        return new FunctionalityCatalog(name, () -> null, Map.of());
    }

    private static FunctionalityGroup group(String first, String second) {
        FunctionalityId firstId = FunctionalityId.forSagaFunctionality(first);
        FunctionalityId secondId = FunctionalityId.forSagaFunctionality(second);
        if (first.compareTo(second) <= 0) {
            return new FunctionalityGroup(firstId, secondId, Set.of());
        }
        return new FunctionalityGroup(secondId, firstId, Set.of());
    }

    private static Orchestrator.PlannedCatalog plan(
            FunctionalityCatalog catalog, FunctionalityGroup... groups) {
        return new Orchestrator.PlannedCatalog(catalog, 4, 10, List.of(groups));
    }
}
