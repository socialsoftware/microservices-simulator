package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StepDependencyGraphTest {

    private static final FunctionalityId FUNC_A = FunctionalityId.forSagaFunctionality("A");
    private static final FunctionalityId FUNC_B = FunctionalityId.forSagaFunctionality("B");

    private static final StepId A1 = StepId.forFunctionalityStep(FUNC_A, "1");
    private static final StepId A2 = StepId.forFunctionalityStep(FUNC_A, "2");
    private static final StepId B1 = StepId.forFunctionalityStep(FUNC_B, "1");
    private static final StepId B2 = StepId.forFunctionalityStep(FUNC_B, "2");

    @Test
    @DisplayName("An empty graph has no paths and admits any single dependency")
    void emptyGraphHasNoPaths() {
        StepDependencyGraph graph = new StepDependencyGraph();

        assertFalse(graph.hasPath(A1, A2));
        assertFalse(graph.wouldCreateCycle(A1, A2));
        assertFalse(graph.isRedundantDependency(A1, A2));
    }

    @Test
    @DisplayName("hasPath follows dependency edges transitively")
    void hasPathIsTransitive() {
        StepDependencyGraph graph = new StepDependencyGraph();
        // A2 depends on A1, B1 depends on A2 => A1 -> A2 -> B1
        graph.addDependency(A2, A1);
        graph.addDependency(B1, A2);

        assertTrue(graph.hasPath(A1, A2));
        assertTrue(graph.hasPath(A1, B1)); // transitive
        assertFalse(graph.hasPath(B1, A1)); // wrong direction
    }

    @Test
    @DisplayName("wouldCreateCycle catches the direct back edge and reflexive edges")
    void wouldCreateCycleCatchesBackEdge() {
        StepDependencyGraph graph = new StepDependencyGraph();
        graph.addDependency(A2, A1); // A1 -> A2

        // "A1 depends on A2" would add A2 -> A1, closing A1 -> A2 -> A1
        assertTrue(graph.wouldCreateCycle(A1, A2));
        // a step depending on itself is always a cycle
        assertTrue(graph.wouldCreateCycle(A1, A1));
        // the reverse (already implied) direction is not a cycle, just redundant
        assertFalse(graph.wouldCreateCycle(A2, A1));
    }

    @Test
    @DisplayName("wouldCreateCycle catches transitive back edges")
    void wouldCreateCycleCatchesTransitiveBackEdge() {
        StepDependencyGraph graph = new StepDependencyGraph();
        // A1 -> A2 -> B1
        graph.addDependency(A2, A1);
        graph.addDependency(B1, A2);

        // "A1 depends on B1" would add B1 -> A1, closing A1 -> A2 -> B1 -> A1
        assertTrue(graph.wouldCreateCycle(A1, B1));
    }

    @Test
    @DisplayName("isRedundantDependency reports already-implied orderings")
    void isRedundantDependencyDetectsImpliedOrdering() {
        StepDependencyGraph graph = new StepDependencyGraph();
        // A1 -> A2 -> B1
        graph.addDependency(A2, A1);
        graph.addDependency(B1, A2);

        // B1 already runs after A1, so "B1 depends on A1" adds nothing
        assertTrue(graph.isRedundantDependency(B1, A1));
        // an unrelated pair is not redundant
        assertFalse(graph.isRedundantDependency(B2, A1));
    }

    @Test
    @DisplayName("addAll imports every key-depends-on-value relation")
    void addAllImportsDependencies() {
        StepDependencies dependencies = new StepDependencies();
        dependencies.addStepDependencies(A2, Set.of(A1));
        dependencies.addStepDependencies(B1, Set.of(A2));

        StepDependencyGraph graph = new StepDependencyGraph().addAll(dependencies);

        assertTrue(graph.hasPath(A1, B1));
        assertTrue(graph.wouldCreateCycle(A1, B1));
    }

    @Test
    @DisplayName("The copy constructor is independent of the original")
    void copyConstructorIsIndependent() {
        StepDependencyGraph original = new StepDependencyGraph();
        original.addDependency(A2, A1); // A1 -> A2

        StepDependencyGraph copy = new StepDependencyGraph(original);
        copy.addDependency(B1, A2); // only in the copy: A2 -> B1

        assertTrue(copy.hasPath(A1, B1));
        assertFalse(original.hasPath(A1, B1));
    }
}
