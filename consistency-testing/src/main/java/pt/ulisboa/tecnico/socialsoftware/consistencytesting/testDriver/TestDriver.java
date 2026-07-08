package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencyGraph;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestCase;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

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
     * Runs the fixed budget of randomized explorations over the test case produced
     * by {@code initialStateSetup} and returns every run's {@link TestResult}.
     * <p>
     * The supplier must return a fresh {@link TestCase.Builder} on each call (it
     * sets up DB state, which the oracle clears after every run). It may already
     * carry baseline inter-dependencies; those flow through untouched, and the
     * driver folds the resulting happens-before edges into its graph so the
     * constraints it injects never contradict them.
     */
    public List<TestResult> exploreTestCase(Supplier<TestCase.Builder> initialStateSetup) {
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

            TestResult result = oracle.runTest(() -> buildTestCase(initialStateSetup, chosen));

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

    Oracle getOracle() {
        return oracle;
    }
}
