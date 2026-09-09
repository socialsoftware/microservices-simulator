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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencyGraph;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestCase;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;
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

    /** For explorations that do not inspect a run's data before it is wiped. */
    private static final Consumer<TestResult> NO_BEFORE_CLEANUP_HOOK = result -> {
        // do nothing
    };

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
     * The directory {@link #exploreGroup} writes a group's reports into, relative
     * to the driver's reports directory.
     */
    public static Path reportsSubdirectoryOf(FunctionalityCatalog catalog, FunctionalityGroup group) {
        return Path.of(StringUtils.toFileNameSafe(catalog.name()), group.label());
    }

    /** A bean of the application under test; only valid after {@link #init()}. */
    public <T> T getApplicationBean(Class<T> beanClass) {
        return oracle.getBean(beanClass);
    }

    /**
     * Utility method, same as {@link #exploreTestCase(Supplier, Consumer)},
     * but does not invoke any {@code beforeCleanupHook} with the run's result and
     * data still in the database.
     */
    public List<TestResult> exploreTestCase(Supplier<TestCase.Builder> initialStateSetup) {
        return exploreTestCase(initialStateSetup, NO_BEFORE_CLEANUP_HOOK);
    }

    /**
     * Same as {@link #exploreTestCase(Supplier, Consumer, Path)}, writing this
     * exploration's reports into the driver's reports directory itself rather
     * than into a subdirectory of it.
     */
    public List<TestResult> exploreTestCase(
            Supplier<TestCase.Builder> initialStateSetup, Consumer<TestResult> beforeCleanupHook) {

        return exploreTestCase(initialStateSetup, beforeCleanupHook, null);
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
     * <p>
     * Every run's report is written into {@code reportSubdirectory} of the driver's
     * reports directory ({@code null} writes into the reports directory itself).
     */
    private List<TestResult> exploreTestCase(
            Supplier<TestCase.Builder> initialStateSetup,
            Consumer<TestResult> beforeCleanupHook,
            @Nullable Path reportSubdirectory) {

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
            reportWriter.write(TestReport.from(result), reportSubdirectory);

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
     * observed steps whose concrete identity remains meaningful after the database
     * is recreated, shuffled; each is admitted only if it keeps the happens-before
     * graph acyclic and is not already implied (redundant).
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
     * Every ordered pair of observed steps belonging to different functionalities,
     * excluding steps whose identity does not remain stable after the database is
     * recreated.
     */
    static List<InterDependency> crossFunctionalityPairs(Set<StepId> observedSteps) {
        List<StepId> crossRunIdentityStableSteps = observedSteps.stream()
                .filter(StepId::isIdentityStableAcrossRuns)
                .toList();

        List<InterDependency> pairs = new ArrayList<>();
        for (StepId dependent : crossRunIdentityStableSteps) {
            for (StepId dependsOn : crossRunIdentityStableSteps) {
                if (dependent.equals(dependsOn)
                        || dependent.getFunctionalityId().equals(dependsOn.getFunctionalityId())) {
                    continue; // self / same-functionality pairs are intra-dependencies, never inter
                }
                pairs.add(new InterDependency(dependent, dependsOn));
            }
        }
        return pairs;
    }

    /**
     * Whether {@code result} is worth a developer's attention: it raised an
     * {@link #INTERESTING_STATUSES interesting status} or a step threw.
     * <p>
     * Detected {@link Anomaly anomalies} deliberately do NOT make a run a finding
     * by themselves — they are reported as evidence, but an anomaly alone does not
     * mean the application misbehaved.
     */
    public static boolean isFinding(TestResult result) {
        return !result.exceptions().isEmpty()
                || result.statuses().stream().anyMatch(INTERESTING_STATUSES::contains);
    }

    /**
     * A solo profiling run raising any of these does not describe the
     * functionality's behaviour — it signals a broken catalog entry or initial
     * state, so profiling fails loudly instead of emitting a garbage footprint.
     * <p>
     * Plain step exceptions also invalidate profiling. Catalog factories must
     * describe ordinary, solo-valid invocations; otherwise their partial and
     * compensation effects are not a trustworthy footprint for pair planning.
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
     * reproducible. Every entry is attempted so one validation reports all
     * invalid entries, then the whole catalog is rejected if any run raised a
     * step exception or profiling-invalidating status.
     */
    public Map<FunctionalityId, FunctionalityFootprint> profileFunctionalities(FunctionalityCatalog catalog) {
        return profileFunctionalities(catalog, true);
    }

    /**
     * Test-only escape hatch for targeted catalogs whose purpose is to explore
     * operations made valid by concurrent progress.
     */
    Map<FunctionalityId, FunctionalityFootprint> profileFunctionalitiesAllowingSoloExceptions(
            FunctionalityCatalog catalog) {

        return profileFunctionalities(catalog, false);
    }

    private Map<FunctionalityId, FunctionalityFootprint> profileFunctionalities(
            FunctionalityCatalog catalog, boolean rejectSoloExceptions) {

        Map<FunctionalityId, FunctionalityFootprint> footprints = new LinkedHashMap<>();
        List<String> profilingFailures = new ArrayList<>();

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
            if (!invalidating.isEmpty() || (rejectSoloExceptions && !result.exceptions().isEmpty())) {
                profilingFailures.add("functionality '%s': statuses=%s, exceptionSteps=%s"
                        .formatted(functionalityId, invalidating, result.exceptions().keySet()));
                continue;
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

        if (!profilingFailures.isEmpty()) {
            throw new IllegalStateException(
                    "Catalog '%s' has %d invalid solo profiling run(s). " +
                            "Every catalog factory and initial state must produce a successful invocation:%n - %s"
                                    .formatted(catalog.name(), profilingFailures.size(),
                                            String.join(System.lineSeparator() + " - ", profilingFailures)));
        }

        return footprints;
    }

    /**
     * Utility method, same as {@link #exploreGroup(FunctionalityCatalog,
     * FunctionalityGroup, Consumer)}, but does not invoke any
     * {@code beforeCleanupHook} with the run's result and data still in the
     * database.
     */
    public List<TestResult> exploreGroup(FunctionalityCatalog catalog, FunctionalityGroup group) {
        return exploreGroup(catalog, group, NO_BEFORE_CLEANUP_HOOK);
    }

    /**
     * Explores one {@link FunctionalityGroup}: builds the group's functionalities
     * from the catalog on a fresh initial state per run, writing the reports into
     * the group's own {@link #reportsSubdirectoryOf subdirectory}.
     * <p>
     * A self-pair instantiates its functionality's factory twice — two
     * independent instances with the same arguments — with the second instance
     * registered under a {@code #2}-suffixed id.
     * <p>
     * {@code beforeCleanupHook} runs on each run's final state, receiving its
     * result and being able to observe that run's data while it's still in the
     * database.
     */
    public List<TestResult> exploreGroup(
            FunctionalityCatalog catalog, FunctionalityGroup group, Consumer<TestResult> beforeCleanupHook) {

        for (FunctionalityId member : group.members()) {
            if (!catalog.funcFactories().containsKey(member)) {
                throw new IllegalArgumentException(
                        "Group member '%s' has no factory in the catalog".formatted(member));
            }
        }

        Supplier<TestCase.Builder> initialStateSetup = () -> {
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
        };

        return exploreTestCase(
                initialStateSetup, beforeCleanupHook, reportsSubdirectoryOf(catalog, group));
    }

    Oracle getOracle() {
        return oracle;
    }
}
