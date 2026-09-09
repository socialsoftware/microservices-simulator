package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityFootprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroup;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroupPlanner;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

/**
 * Runs a whole consistency-testing campaign over an application, end to end:
 * boots it, asks it what to test, and explores everything worth exploring.
 * <p>
 * For each catalog the application's {@link FunctionalityCatalogsProvider}
 * exposes:
 * <ol>
 * <li>profile every functionality alone
 * ({@link TestDriver#profileFunctionalities}), obtaining its read/write
 * footprint;</li>
 * <li>plan the pairs worth running concurrently
 * ({@link FunctionalityGroupPlanner});</li>
 * <li>explore each planned group for a fixed budget of randomized schedules
 * ({@link TestDriver#exploreGroup});</li>
 * <li>collect every run worth attention into an
 * {@link OrchestrationReport}.</li>
 * </ol>
 * 
 * Typical use, from the application's own test sources:
 *
 * <pre>{@code
 * OrchestrationReport report = Orchestrator.of(MyAppSimulator.class)
 *         .withIterationsPerGroup(20)
 *         .withReportsDirectory(Path.of("target/consistency-reports"))
 *         .run();
 * }</pre>
 *
 * The application must also expose an {@code InterInvariantsProvider} bean;
 * both providers are {@code @Profile("oracle")} beans the tool activates
 * itself.
 */
public final class Orchestrator {

    private static final Logger log = LoggerFactory.getLogger(Orchestrator.class);

    private static final int DEFAULT_ITERATIONS_PER_GROUP = 20;
    private static final long DEFAULT_MASTER_SEED = 42L;
    private static final Path DEFAULT_REPORTS_DIRECTORY = Path.of("target", "consistency-reports");
    private static final String IGNORED_SEMANTIC_LOCKS_PROPERTY = "consistency.ignoredSemanticLocks";
    private static final String REPORTS_DIRECTORY_PROPERTY = "consistency.reportsDirectory";

    private static final String RUN_REPORT_FILE_NAME = "test-report-%05d.json";

    /**
     * A sweep should show its own checkpoints, not every application command and
     * framework lifecycle message.
     */
    private static final List<String> DEFAULT_SWEEP_LOGGING_ARGS = List.of(
            "--logging.level.root=WARN",
            "--logging.level.pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator=INFO",
            "--logging.level.org.springframework.scheduling.support=OFF",
            "--spring.main.banner-mode=off");

    private final Class<?> springAppClass;

    private List<String> springAppArgs = List.of();
    private int iterationsPerGroup = DEFAULT_ITERATIONS_PER_GROUP;
    private long masterSeed = DEFAULT_MASTER_SEED;
    private Path reportsDirectory = DEFAULT_REPORTS_DIRECTORY;
    private Set<SemanticLockId> ignoredSemanticLocks = Set.of();

    private Orchestrator(Class<?> springAppClass) {
        this.springAppClass = springAppClass;
    }

    public static Orchestrator of(Class<?> springAppClass) {
        Orchestrator orchestrator = new Orchestrator(springAppClass);
        String configuredIgnoredLocks = System.getProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY);
        if (configuredIgnoredLocks != null && !configuredIgnoredLocks.isBlank()) {
            orchestrator.withIgnoredSemanticLockSelectors(
                    Arrays.stream(configuredIgnoredLocks.split(",", -1)).map(String::trim).toList());
        }
        return orchestrator;
    }

    /**
     * Extra Spring arguments for the application under test. The oracle's own
     * arguments (profiles, datasource, ...) always win over these.
     */
    public Orchestrator withSpringAppArgs(List<String> springAppArgs) {
        this.springAppArgs = List.copyOf(springAppArgs);
        return this;
    }

    /**
     * Oracle runs per planned group. Every planned group gets exactly this many.
     */
    public Orchestrator withIterationsPerGroup(int iterationsPerGroup) {
        if (iterationsPerGroup < 1) {
            throw new IllegalArgumentException("iterationsPerGroup must be >= 1, got " + iterationsPerGroup);
        }
        this.iterationsPerGroup = iterationsPerGroup;
        return this;
    }

    /** Fix it to reproduce a campaign, vary it to diversify across campaigns. */
    public Orchestrator withMasterSeed(long masterSeed) {
        this.masterSeed = masterSeed;
        return this;
    }

    /**
     * Where per-run reports and the campaign summary are written. The
     * {@value #REPORTS_DIRECTORY_PROPERTY} system property overrides this value.
     */
    public Orchestrator withReportsDirectory(Path reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
        return this;
    }

    /**
     * Configures test-only fault injection for a sweep. For each selector, the
     * text before {@code #} is the fully qualified name of a saga-state enum and
     * the text after it is the state's {@link SagaState#getStateName() state name}.
     * A matching acquisition is skipped, modeling code that forgot to acquire
     * that semantic lock. Every selector is validated before the application
     * starts, so a typo cannot silently run a no-fault sweep.
     *
     * @throws IllegalArgumentException if any selector is malformed, its class
     *                                  cannot be loaded, does not implement
     *                                  {@link SagaState}, is not an
     *                                  enum, or does not contain the requested
     *                                  state name
     */
    public Orchestrator withIgnoredSemanticLockSelectors(Collection<String> selectors) {
        Set<SemanticLockId> parsedSelectors = new HashSet<>();
        for (String selector : selectors) {
            SemanticLockId semanticLock = SemanticLockId.parse(selector.trim());
            semanticLock.validateAgainst(springAppClass.getClassLoader());
            parsedSelectors.add(semanticLock);
        }
        this.ignoredSemanticLocks = Set.copyOf(parsedSelectors);
        return this;
    }

    /*
     * TODO budget strategy: every planned group currently gets exactly
     * `iterationsPerGroup` runs, so a large catalog's campaign grows without bound
     * (groups x iterations). Worth exploring once there is a baseline to compare
     * against:
     * - a wall-clock deadline, checked BETWEEN groups so partial campaigns stay
     * interpretable (the summary already reports `unexploredGroups`);
     * - round-robin over groups, so every group gets some attention before any
     * group gets more;
     * - adaptive budgets, giving more runs to groups that already produced
     * findings.
     */

    /**
     * Runs the campaign: boots the application, explores every catalog it
     * exposes, writes the reports and returns what was found.
     */
    public OrchestrationReport run() {
        long startedAt = System.currentTimeMillis();
        Path effectiveReportsDirectory = effectiveReportsDirectory(startedAt);
        List<String> effectiveSpringAppArgs = defaultedSpringAppArgs(springAppArgs);

        TestDriver driver = new TestDriver(springAppClass, effectiveSpringAppArgs, effectiveReportsDirectory)
                .setIterations(iterationsPerGroup)
                .setMasterSeed(masterSeed);

        CampaignProgress progress = new CampaignProgress(
                springAppClass.getName(), masterSeed, effectiveSpringAppArgs, iterationsPerGroup,
                StringUtils.toPortableString(effectiveReportsDirectory), ignoredSemanticLockSelectors(), startedAt);

        CampaignSummaryWriter summaryWriter = new CampaignSummaryWriter(effectiveReportsDirectory);

        CampaignCheckpoint checkpoint = new CampaignCheckpoint(progress, summaryWriter);
        Thread shutdownCheckpoint = new Thread(checkpoint::cancel, "consistency-sweep-summary-checkpoint");
        Runtime.getRuntime().addShutdownHook(shutdownCheckpoint);

        try {
            checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);
            driver.init();

            List<FunctionalityCatalog> catalogs = getCatalogs(driver);
            log.info("Campaign over {}: {} catalog(s), {} iteration(s) per group, master seed {}",
                    springAppClass.getSimpleName(), catalogs.size(), iterationsPerGroup, masterSeed);

            List<PlannedCatalog> plans = catalogs.stream().map(catalog -> planCatalog(driver, catalog)).toList();
            progress.setPlanHash(planHashOf(plans));
            checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);

            // Fault injection starts only after every catalog has been profiled and
            // planned. A fault-injected campaign therefore explores the same plan a normal
            // campaign would produce for this catalog/configuration/seed as intended.
            driver.setIgnoredSemanticLocks(ignoredSemanticLocks);

            for (PlannedCatalog plan : plans) {
                exploreCatalog(driver, plan, progress, checkpoint);
            }

            OrchestrationReport report = checkpoint.write(
                    OrchestrationReport.CampaignStatus.COMPLETED, System.currentTimeMillis());
            log.info("Campaign finished:{}{}", System.lineSeparator(), report.summary());
            return report;
        } catch (RuntimeException | Error e) {
            checkpoint.write(OrchestrationReport.CampaignStatus.FAILED, System.currentTimeMillis());
            throw e;
        } finally {
            driver.shutdown();
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownCheckpoint);
            } catch (IllegalStateException ignored) {
                // The shutdown hook is running and has written the cancellation checkpoint.
            }
        }
    }

    /**
     * Profiles and plans one catalog.
     */
    private PlannedCatalog planCatalog(TestDriver driver, FunctionalityCatalog catalog) {

        /*
         * Profiling is deliberately fail-fast: a functionality whose SOLO run
         * already breaks means a broken catalog entry or initial state, and any
         * footprint derived from it would silently misdirect the planner.
         *
         * TODO quarantine instead: profile every entry, exclude the ones that
         * failed from planning, and report them as catalog errors, so one stale
         * entry does not stop the whole campaign.
         */
        Map<FunctionalityId, FunctionalityFootprint> footprints = driver.profileFunctionalities(catalog);

        List<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(footprints.values()).stream()
                .sorted(Comparator.comparing(FunctionalityGroup::label))
                .toList();

        // Unordered pairs plus self-pairs: n*(n-1)/2 + n. What brute force would run.
        int poolSize = footprints.size();
        int possiblePairs = poolSize * (poolSize - 1) / 2 + poolSize;

        log.info("Catalog '{}': profiled {} functionalities, planned {} of {} possible pairs",
                catalog.name(), footprints.size(), groups.size(), possiblePairs);

        return new PlannedCatalog(catalog, footprints.size(), possiblePairs, groups);
    }

    /** Explores a planned catalog, checkpointing it after each completed group. */
    private void exploreCatalog(
            TestDriver driver, PlannedCatalog plan, CampaignProgress progress, CampaignCheckpoint checkpoint) {

        FunctionalityCatalog catalog = plan.catalog();
        List<FunctionalityGroup> groups = plan.groups();

        progress.registerCatalog(catalog.name(), plan.functionalitiesProfiled(), plan.possiblePairs(), groups.size());
        OrchestrationReport catalogCheckpoint = checkpoint.write(
                OrchestrationReport.CampaignStatus.RUNNING, null);
        log.info("{}", CampaignProgressDisplay.format(catalogCheckpoint));

        // TODO progress tracking: plan every catalog before exploration so a
        // partial summary can show campaign-wide, rather than discovered-so-far,
        // totals (relevant for multi-catalog campaigns).
        for (FunctionalityGroup group : groups) {
            List<TestResult> results = driver.exploreGroup(catalog, group, progress::recordCompletedRun);

            List<OrchestrationReport.Finding> groupFindings = findingsOf(catalog, group, results);
            progress.recordCompletedGroup(
                    catalog.name(), summaryOf(group, results, groupFindings.size()), groupFindings);
            OrchestrationReport summary = checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);

            log.info("Catalog '{}', group '{}': {} run(s), {} finding(s)",
                    catalog.name(), group.label(), results.size(), groupFindings.size());
            log.info("{}", CampaignProgressDisplay.format(summary));
        }
    }

    private List<OrchestrationReport.Finding> findingsOf(
            FunctionalityCatalog catalog, FunctionalityGroup group, List<TestResult> results) {

        List<OrchestrationReport.Finding> findings = new ArrayList<>();
        for (int runIndex = 0; runIndex < results.size(); runIndex++) {
            TestResult result = results.get(runIndex);
            if (!TestDriver.isFinding(result)) {
                continue;
            }

            findings.add(new OrchestrationReport.Finding(
                    catalog.name(),
                    group.label(),
                    runIndex,
                    result.statuses().stream().map(Enum::name).sorted().toList(),
                    result.anomalies().stream().map(anomaly -> anomaly.type().name()).distinct().sorted().toList(),
                    result.exceptions().keySet().stream().map(Object::toString).sorted().toList(),
                    StringUtils.toPortableString(resolveRunReportPath(catalog, group, runIndex))));
        }
        return findings;
    }

    /**
     * Where the run's full report was written, relative to the reports directory.
     * Reports are numbered from 1 within each group's directory, in exploration
     * order, so the run's index determines its file.
     * 
     * @param runIndex 0-based index of the run within its group
     */
    private static Path resolveRunReportPath(FunctionalityCatalog catalog, FunctionalityGroup group, int runIndex) {
        return TestDriver.reportsSubdirectoryOf(catalog, group)
                .resolve(RUN_REPORT_FILE_NAME.formatted(runIndex + 1));
    }

    private Path effectiveReportsDirectory(long startedAtEpochMillis) {
        return resolveReportsDirectory(
                reportsDirectory,
                ignoredSemanticLocks,
                System.getProperty(REPORTS_DIRECTORY_PROPERTY),
                Instant.ofEpochMilli(startedAtEpochMillis),
                UUID.randomUUID());
    }

    /**
     * Chooses where to write reports. An explicit system property always wins.
     * Otherwise, normal-runs and caller-supplied directories keep the requested
     * path; experiment-runs using the normal-runs default path get a unique
     * experiment directory instead.
     */
    static Path resolveReportsDirectory(
            Path requestedDirectory,
            Set<SemanticLockId> ignoredLocks,
            String reportsDirectoryProperty,
            Instant startedAt,
            UUID runId) {

        if (reportsDirectoryProperty != null && !reportsDirectoryProperty.isBlank()) {
            return Path.of(reportsDirectoryProperty);
        }
        if (!isExperiment(ignoredLocks) || !requestedDirectory.equals(DEFAULT_REPORTS_DIRECTORY)) {
            return requestedDirectory;
        }
        return ExperimentReportsDirectory.pathForExperiment(startedAt, runId);
    }

    private static boolean isExperiment(Set<SemanticLockId> ignoredLocks) {
        return !ignoredLocks.isEmpty();
    }

    private List<String> ignoredSemanticLockSelectors() {
        return ignoredSemanticLocks.stream().map(SemanticLockId::toSelector).sorted().toList();
    }

    private static String planHashOf(List<PlannedCatalog> plans) {
        String canonicalPlan = plans.stream()
                .map(PlannedCatalog::canonicalForm)
                .collect(java.util.stream.Collectors.joining("\n"));
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonicalPlan.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to fingerprint a consistency campaign plan", e);
        }
    }

    /**
     * Adds sweep logging defaults, while letting caller-supplied command-line
     * properties override the same default property. The returned values are the
     * exact Spring arguments recorded in the campaign summary.
     */
    static List<String> defaultedSpringAppArgs(List<String> applicationArgs) {
        Map<String, String> argumentsByProperty = new LinkedHashMap<>();
        DEFAULT_SWEEP_LOGGING_ARGS.forEach(argument -> argumentsByProperty.put(argumentProperty(argument), argument));
        applicationArgs.forEach(argument -> argumentsByProperty.put(argumentProperty(argument), argument));
        return List.copyOf(argumentsByProperty.values());
    }

    private static String argumentProperty(String argument) {
        if (!argument.startsWith("--")) {
            return argument;
        }
        int assignment = argument.indexOf('=');
        return assignment < 0 ? argument : argument.substring(0, assignment);
    }

    private static OrchestrationReport.GroupSummary summaryOf(
            FunctionalityGroup group, List<TestResult> results, int findingCount) {

        Map<String, Integer> anomalyCounts = new LinkedHashMap<>();
        for (TestResult result : results) {
            result.anomalies().stream().map(Anomaly::type).map(Enum::name).distinct()
                    .forEach(type -> anomalyCounts.merge(type, 1, Integer::sum));
        }

        return new OrchestrationReport.GroupSummary(
                group.label(), group.first().toString(), group.second().toString(), group.isSelfPair(),
                group.conflicts().stream()
                        .map(FunctionalityGroup.Conflict::identity).sorted().toList(),
                results.size(), findingCount, Map.copyOf(anomalyCounts));
    }

    /**
     * The application's {@link FunctionalityCatalogsProvider} catalogs, failing
     * with the blueprint of what is missing when the application does not expose
     * one.
     */
    private List<FunctionalityCatalog> getCatalogs(TestDriver driver) {
        FunctionalityCatalogsProvider provider;
        try {
            provider = driver.getApplicationBean(FunctionalityCatalogsProvider.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalStateException("""
                    SpringBoot application [%s] is missing bean implementation for [%s].
                    Please add a component in the application's 'src/test/java/...' matching this blueprint:

                    @Component
                    @Profile("oracle")
                    public class TestFunctionalityCatalogsProvider implements FunctionalityCatalogsProvider { ... }
                    """.formatted(springAppClass.getName(), FunctionalityCatalogsProvider.class.getName()), e);
        }

        List<FunctionalityCatalog> catalogs = provider.getCatalogs();
        if (catalogs.isEmpty()) {
            throw new IllegalStateException(
                    "[%s] returned no catalogs: there is nothing to explore".formatted(provider.getClass().getName()));
        }

        List<String> names = catalogs.stream().map(FunctionalityCatalog::name).toList();
        if (names.size() != names.stream().distinct().count()) {
            throw new IllegalStateException(
                    "Catalog names must be unique (they name the reports directories), got: " + names);
        }
        return catalogs;
    }

    private record PlannedCatalog(
            FunctionalityCatalog catalog,
            int functionalitiesProfiled,
            int possiblePairs,
            List<FunctionalityGroup> groups) {

        private PlannedCatalog {
            groups = List.copyOf(groups);
        }

        private String canonicalForm() {
            return "%s|%d|%d\n%s".formatted(
                    catalog.name(),
                    functionalitiesProfiled,
                    possiblePairs,
                    groups.stream()
                            .map(PlannedCatalog::canonicalGroupOf)
                            .collect(java.util.stream.Collectors.joining("\n")));
        }

        private static String canonicalGroupOf(FunctionalityGroup group) {
            return "%s|%s|%s|%s".formatted(
                    group.label(),
                    group.first(),
                    group.second(),
                    group.conflicts().stream()
                            .sorted(Comparator.comparing(FunctionalityGroup.Conflict::identity))
                            .map(PlannedCatalog::canonicalConflictOf)
                            .collect(java.util.stream.Collectors.joining(",")));
        }

        private static String canonicalConflictOf(FunctionalityGroup.Conflict conflict) {
            return "%s:%s:%s".formatted(
                    conflict.identity(),
                    sortedEnumNames(conflict.firstMemberKinds()),
                    sortedEnumNames(conflict.secondMemberKinds()));
        }

        private static List<String> sortedEnumNames(Set<? extends Enum<?>> values) {
            return values.stream().map(Enum::name).sorted().toList();
        }
    }

}
