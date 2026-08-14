package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencyGraph;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestCase;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public final class TestDriver {

    /**
     * A cross-functionality ordering constraint: {@code dependent} runs after
     * {@code dependsOn}.
     */
    record InterDependency(StepId dependent, StepId dependsOn) {
    }

    private static final Logger log = LoggerFactory.getLogger(TestDriver.class);

    /** Default number of oracle runs per {@link #exploreTestCase} call. */
    private static final int DEFAULT_ITERATIONS = 30;

    /**
     * Fixed by default so an exploration is reproducible
     * (seeds the whole random schedule).
     */
    private static final long DEFAULT_MASTER_SEED = 42L;

    /**
     * Cap on inter-dependencies injected per run: more constraints make a run
     * likelier to be unsatisfiable.
     */
    private static final int MAX_INTER_DEPENDENCIES_PER_RUN = 10; // TODO experiment and change

    /**
     * A run exhibiting any of these (or a thrown step exception) is reported as a
     * potential finding.
     */
    private static final Set<TestStatus> INTERESTING_STATUSES = Set.of(
            TestStatus.INTERNAL_SYSTEM_EXCEPTION,
            TestStatus.CRITICAL_STEP_FAILURE,
            TestStatus.INTER_INVARIANT_VIOLATION,
            TestStatus.EXECUTION_LIMIT_EXCEEDED);

    private final Oracle oracle;
    private final TestReportWriter reportWriter;

    private int iterations = DEFAULT_ITERATIONS;
    private long masterSeed = DEFAULT_MASTER_SEED;

    public TestDriver(Class<?> springAppClass, List<String> springAppBaseArgs, Path reportsDirectory) {
        oracle = new Oracle(springAppClass, springAppBaseArgs);
        reportWriter = new TestReportWriter(reportsDirectory);
    }

    public void init() {
        oracle.init();
    }

    public void shutdown() {
        oracle.shutdown();
    }

    /**
     * Sets the number of oracle runs per {@link #exploreTestCase} call.
     * <p>
     * Default is {@link #DEFAULT_ITERATIONS}.
     */
    public TestDriver setIterations(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be >= 1, got " + iterations);
        }
        this.iterations = iterations;
        return this;
    }

    /**
     * Overrides the master seed; fix it to reproduce an exploration, vary it to
     * diversify across explorations.
     * <p>
     * Default is {@link #DEFAULT_MASTER_SEED}.
     */
    public TestDriver setMasterSeed(long masterSeed) {
        this.masterSeed = masterSeed;
        return this;
    }

    /**
     * Utility method, same as {@link #exploreTestCase(Supplier, Consumer)},
     * but does not invoke any {@code beforeCleanupHook} with the run's result and
     * data still in the database.
     */
    public List<TestResult> exploreTestCase(Supplier<TestCase.Builder> initialStateSetup) {
        return exploreTestCase(initialStateSetup, result -> {
            // do nothing
        });
    }

    /**
     * Runs the fixed budget of randomized explorations over the test case produced
     * by {@code initialStateSetup} and returns every run's {@link TestResult}.
     * <p>
     * The supplier must return a fresh {@link TestCase.Builder} on each call (it
     * sets up DB state, which the oracle clears after every run). It may already
     * carry baseline inter-dependencies; those flow through untouched, and the
     * driver folds the resulting happens-before edges into its graph so the
     * constraints it injects never contradict them.
     * <p>
     * Invokes {@code beforeCleanupHook} with each run's result while that run's
     * data is still in the database, which is the only moment a caller can inspect
     * the final state a schedule left behind: the oracle wipes the database as soon
     * as the run returns.
     */
    public List<TestResult> exploreTestCase(
            Supplier<TestCase.Builder> initialStateSetup, Consumer<TestResult> beforeCleanupHook) {

        Random rng = new Random(masterSeed);
        List<TestResult> results = new ArrayList<>();

        Set<StepId> observedSteps = new HashSet<>();
        StepDependencies observedIntraDependencies = new StepDependencies();
        StepDependencies observedInterDependencies = new StepDependencies();

        // int runsWithFindings = 0; // TODO could make sense to track

        for (int iteration = 0; iteration < iterations; iteration++) {
            // Each run gets a fresh scheduler seed so the same constraints can still
            // realize different concrete interleavings.
            oracle.setSchedulerSeed(rng.nextLong());

            Set<InterDependency> chosen = chooseInterDependencies(
                    observedSteps, observedIntraDependencies, observedInterDependencies, rng);

            TestResult result = oracle.runTest(
                    () -> buildTestCase(initialStateSetup, chosen), beforeCleanupHook);

            results.add(result);
            reportWriter.write(TestReport.from(result));

            observedSteps.addAll(result.schedule());
            observedSteps.addAll(result.intraDependencies().getSteps());
            observedIntraDependencies.merge(result.intraDependencies());
            observedInterDependencies.merge(result.interDependencies());

            if (isFinding(result)) {
                // runsWithFindings++; // TODO could make sense to track
                log.warn("Run {}: potential issue found, statuses={}, exceptions={}",
                        iteration, result.statuses(), result.exceptions().keySet());
            }
        }

        return results;
    }

    /**
     * Applies the chosen inter-dependencies on top of a fresh builder. Any single
     * constraint the builder rejects (a residual cycle or rule violation the
     * candidate generation did not foresee) is dropped and logged: the run still
     * proceeds with the rest.
     */
    private TestCase buildTestCase(Supplier<TestCase.Builder> initialStateSetup, Set<InterDependency> chosen) {
        TestCase.Builder builder = initialStateSetup.get();

        for (InterDependency interDep : chosen) {
            try {
                builder.addInterDependency(interDep.dependent(), interDep.dependsOn());
            } catch (RuntimeException e) {
                log.debug("Dropping inter-dependency {} rejected by the builder: {}", interDep, e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * Picks a random, jointly-satisfiable set of {@code 0..cap} inter-dependencies
     * to inject next. Candidates are every ordered cross-functionality pair of
     * observed steps, shuffled; each is admitted only if it keeps the
     * happens-before graph acyclic and is not already implied (redundant).
     */
    private Set<InterDependency> chooseInterDependencies(
            Set<StepId> observedSteps,
            StepDependencies observedIntraDependencies,
            StepDependencies observedInterDependencies,
            Random rng) {

        List<InterDependency> candidates = crossFunctionalityPairs(observedSteps);
        if (candidates.isEmpty()) {
            return Set.of();
        }
        Collections.shuffle(candidates, rng);

        int cap = Math.min(MAX_INTER_DEPENDENCIES_PER_RUN, candidates.size());
        int target = rng.nextInt(cap + 1); // uniform 0..cap; 0 means a pure scheduler-seed run
        if (target == 0) {
            return Set.of();
        }

        StepDependencyGraph depGraph = new StepDependencyGraph();
        depGraph.addAll(observedIntraDependencies);
        depGraph.addAll(observedInterDependencies);

        // LinkedHashSet to preserve the order of injection for reproducibility
        Set<InterDependency> chosen = new LinkedHashSet<>();
        for (InterDependency candidate : candidates) {
            if (chosen.size() >= target) {
                break;
            }
            StepId dependent = candidate.dependent();
            StepId dependsOn = candidate.dependsOn();

            if (depGraph.wouldCreateCycle(dependent, dependsOn)
                    || depGraph.isRedundantDependency(dependent, dependsOn)) {
                continue;
            }

            depGraph.addDependency(dependent, dependsOn);
            chosen.add(candidate);
        }

        return chosen;
    }

    /**
     * Every ordered pair of observed steps belonging to different functionalities.
     */
    private static List<InterDependency> crossFunctionalityPairs(Set<StepId> observedSteps) {
        List<InterDependency> pairs = new ArrayList<>();
        for (StepId dependent : observedSteps) {
            for (StepId dependsOn : observedSteps) {
                if (dependent.equals(dependsOn)
                        || dependent.getFunctionalityId().equals(dependsOn.getFunctionalityId())) {
                    continue; // self / same-functionality pairs are intra-dependencies, never inter
                }
                pairs.add(new InterDependency(dependent, dependsOn));
            }
        }
        return pairs;
    }

    private static boolean isFinding(TestResult result) {
        return !result.exceptions().isEmpty()
                || result.statuses().stream().anyMatch(INTERESTING_STATUSES::contains);
    }

    /**
     * A solo profiling run raising any of these does not describe the
     * functionality's behaviour — it signals a broken catalog entry or initial
     * state, so profiling fails loudly instead of emitting a garbage footprint.
     * <p>
     * Plain step exceptions are deliberately NOT rejected: a functionality that
     * aborts and compensates even when running alone (a business-rule
     * rejection) is a legitimate catalog entry, and capturing its solo run —
     * compensation writes included — is exactly its footprint.
     */
    private static final Set<TestStatus> PROFILING_INVALIDATING_STATUSES = Set.of(
            TestStatus.INTERNAL_SYSTEM_EXCEPTION,
            TestStatus.CRITICAL_STEP_FAILURE,
            TestStatus.EXECUTION_LIMIT_EXCEEDED,
            TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED,
            TestStatus.INTER_INVARIANT_VIOLATION);

    /**
     * Runs each functionality of {@code catalog} ALONE, once, on a fresh
     * instance of the catalog's initial state, and returns its observed
     * {@link FunctionalityFootprint}.
     * <p>
     * Solo runs use the fixed master seed: with no concurrency in the schedule
     * the footprint is expected to be stable, and a fixed seed keeps profiling
     * reproducible.
     */
    public Map<FunctionalityId, FunctionalityFootprint> profileFunctionalities(FunctionalityCatalog catalog) {
        Map<FunctionalityId, FunctionalityFootprint> footprints = new LinkedHashMap<>();

        for (var entry : catalog.funcFactories().entrySet()) {
            FunctionalityId functionalityId = entry.getKey();
            Function<AggregateHandlesRegistry, WorkflowFunctionality> factory = entry.getValue();

            oracle.setSchedulerSeed(masterSeed);

            // The registry only exists once the run's initial state was set up,
            // but is needed after the run to resolve effects to handles.
            AtomicReference<AggregateHandlesRegistry> registryRef = new AtomicReference<>();

            TestResult result = oracle.runTest(() -> {
                AggregateHandlesRegistry registry = catalog.initialStateSetup().get();
                registryRef.set(registry);
                return new TestCase.Builder()
                        .addFunctionality(functionalityId, factory.apply(registry))
                        .build();
            });

            Set<TestStatus> invalidating = new HashSet<>(result.statuses());
            invalidating.retainAll(PROFILING_INVALIDATING_STATUSES);
            if (!invalidating.isEmpty()) {
                throw new IllegalStateException(
                        "Solo profiling run of functionality '%s' raised %s: the catalog entry or the initial state is broken, exceptions=%s"
                                .formatted(functionalityId, invalidating, result.exceptions().keySet()));
            }

            FunctionalityFootprint footprint = FunctionalityFootprint.fromSoloRun(
                    functionalityId, result, registryRef.get());
            footprints.put(functionalityId, footprint);
            log.info("Profiled functionality '{}': hasWrites={}, writes=[{}], reads=[{}]",
                    functionalityId, footprint.writesAnything(),
                    footprint.accesses().stream().filter(a -> a.isWrite()).map(a -> a.identity())
                            .collect(Collectors.joining(", ")),
                    footprint.accesses().stream().filter(a -> !a.isWrite()).map(a -> a.identity())
                            .collect(Collectors.joining(", ")));
        }

        return footprints;
    }

    /**
     * Explores one {@link FunctionalityGroup}: builds the group's functionalities
     * from the catalog on a fresh initial state per run and delegates to
     * {@link #exploreTestCase}.
     * <p>
     * A self-pair instantiates its functionality's factory twice — two
     * independent instances with the same arguments — with the second instance
     * registered under a {@code #2}-suffixed id.
     */
    public List<TestResult> exploreGroup(FunctionalityCatalog catalog, FunctionalityGroup group) {
        for (FunctionalityId member : group.members()) {
            if (!catalog.funcFactories().containsKey(member)) {
                throw new IllegalArgumentException(
                        "Group member '%s' has no factory in the catalog".formatted(member));
            }
        }

        return exploreTestCase(() -> {
            AggregateHandlesRegistry registry = catalog.initialStateSetup().get();

            TestCase.Builder builder = new TestCase.Builder();
            Map<FunctionalityId, Integer> occurrences = new HashMap<>();
            for (FunctionalityId member : group.members()) {
                int occurrence = occurrences.merge(member, 1, Integer::sum);
                FunctionalityId instanceId = occurrence == 1
                        ? member
                        : FunctionalityId.forSagaFunctionality(member + "#" + occurrence);
                builder.addFunctionality(instanceId, catalog.funcFactories().get(member).apply(registry));
            }
            return builder;
        });
    }

    Oracle getOracle() {
        return oracle;
    }
}
