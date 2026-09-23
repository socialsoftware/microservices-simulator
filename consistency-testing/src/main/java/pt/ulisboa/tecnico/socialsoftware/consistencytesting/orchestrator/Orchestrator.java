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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralCoverage;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityFootprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroup;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroupPlanner;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.ScheduleExplorationStrategy;
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
 * <li>explore planned groups using either fixed or adaptive schedule budgets
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
    private static final long ADAPTIVE_BUDGET_SEED_SALT = 0xD1B54A32D192ED03L;
    private static final Path DEFAULT_REPORTS_DIRECTORY = Path.of("target", "consistency-reports");
    private static final String IGNORED_SEMANTIC_LOCKS_PROPERTY = "consistency.ignoredSemanticLocks";
    private static final String GROUP_SELECTORS_PROPERTY = "consistency.groupSelectors";
    private static final String REPORTS_DIRECTORY_PROPERTY = "consistency.reportsDirectory";
    private static final String SCHEDULE_EXPLORATION_PROPERTY = "consistency.scheduleExploration";
    private static final String GROUP_BUDGET_STRATEGY_PROPERTY = "consistency.groupBudgetStrategy";
    private static final String TOTAL_RUN_BUDGET_PROPERTY = "consistency.totalRunBudget";
    private static final String MINIMUM_RUNS_PER_GROUP_PROPERTY = "consistency.minimumRunsPerGroup";
    private static final String MAXIMUM_RUNS_PER_GROUP_PROPERTY = "consistency.maximumRunsPerGroup";
    private static final String ALLOCATION_BATCH_SIZE_PROPERTY = "consistency.allocationBatchSize";

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
    private Set<GroupSelector> groupSelectors = Set.of();
    private ScheduleExplorationStrategy scheduleExplorationStrategy = ScheduleExplorationStrategy.RANDOM_CONSTRAINTS;
    private GroupBudgetStrategy groupBudgetStrategy = GroupBudgetStrategy.FIXED_PER_GROUP;
    // Adaptive-only settings; fixed mode uses iterationsPerGroup instead.
    private int totalRunBudget;
    private int minimumRunsPerGroup;
    private int maximumRunsPerGroup;
    private int allocationBatchSize;

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
        String configuredGroups = System.getProperty(GROUP_SELECTORS_PROPERTY);
        if (configuredGroups != null && !configuredGroups.isBlank()) {
            orchestrator.withGroupSelectors(
                    Arrays.stream(configuredGroups.split(",", -1)).map(String::trim).toList());
        }
        String configuredScheduleExploration = System.getProperty(SCHEDULE_EXPLORATION_PROPERTY);
        if (configuredScheduleExploration != null && !configuredScheduleExploration.isBlank()) {
            orchestrator.withScheduleExplorationStrategy(
                    ScheduleExplorationStrategy.parse(configuredScheduleExploration));
        }
        String configuredGroupBudget = System.getProperty(GROUP_BUDGET_STRATEGY_PROPERTY);
        if (configuredGroupBudget != null && !configuredGroupBudget.isBlank()) {
            GroupBudgetStrategy strategy = GroupBudgetStrategy.parse(configuredGroupBudget);
            if (strategy == GroupBudgetStrategy.ADAPTIVE_NOVELTY) {
                orchestrator.withAdaptiveGroupBudget(
                        requiredPositiveIntegerProperty(TOTAL_RUN_BUDGET_PROPERTY),
                        requiredPositiveIntegerProperty(MINIMUM_RUNS_PER_GROUP_PROPERTY),
                        requiredPositiveIntegerProperty(MAXIMUM_RUNS_PER_GROUP_PROPERTY),
                        requiredPositiveIntegerProperty(ALLOCATION_BATCH_SIZE_PROPERTY));
            }
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
     * Oracle runs per planned group under fixed budgeting. Adaptive budgeting uses
     * its explicit total/minimum/maximum configuration instead.
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

    /**
     * Restricts exploration to the specified planned groups. Each selector has form
     * {@code catalog/group-label}, using the catalog name and group label shown in
     * campaign reports. Empty means explore every planned group.
     * <p>
     * Selected catalogs are identified before profiling, avoiding unrelated solo
     * runs. Group selection happens only after normal, fault-free profiling and
     * planning. Every selector must match exactly one planned group; typos fail the
     * campaign instead of silently exploring nothing.
     */
    public Orchestrator withGroupSelectors(Collection<String> selectors) {
        Set<GroupSelector> parsedSelectors = new HashSet<>();
        for (String selector : selectors) {
            parsedSelectors.add(GroupSelector.parse(selector));
        }
        this.groupSelectors = Set.copyOf(parsedSelectors);
        return this;
    }

    /** Selects within-group schedule exploration. */
    public Orchestrator withScheduleExplorationStrategy(ScheduleExplorationStrategy strategy) {
        this.scheduleExplorationStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    /**
     * Shares one bounded run budget across every selected group. Each group first receives
     * {@code minimumRunsPerGroup}; remaining batches are distributed dynamically according to results.
     */
    public Orchestrator withAdaptiveGroupBudget(
            int totalRunBudget,
            int minimumRunsPerGroup,
            int maximumRunsPerGroup,
            int allocationBatchSize) {

        if (totalRunBudget < 1 || minimumRunsPerGroup < 1
                || maximumRunsPerGroup < minimumRunsPerGroup || allocationBatchSize < 1) {
            throw new IllegalArgumentException(
                    "Adaptive budget requires total >= 1, minimum >= 1, maximum >= minimum, and batch >= 1");
        }
        this.groupBudgetStrategy = GroupBudgetStrategy.ADAPTIVE_NOVELTY;
        this.totalRunBudget = totalRunBudget;
        this.minimumRunsPerGroup = minimumRunsPerGroup;
        this.maximumRunsPerGroup = maximumRunsPerGroup;
        this.allocationBatchSize = allocationBatchSize;
        return this;
    }

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
                .setMasterSeed(masterSeed)
                .setScheduleExplorationStrategy(scheduleExplorationStrategy);

        CampaignProgress progress = new CampaignProgress(
                springAppClass.getName(), masterSeed, effectiveSpringAppArgs, iterationsPerGroup,
                scheduleExplorationStrategy.propertyValue(), groupBudgetStrategy.propertyValue(),
                StringUtils.toPortableString(effectiveReportsDirectory), ignoredSemanticLockSelectors(),
                configuredGroupSelectors(), startedAt);

        if (groupBudgetStrategy == GroupBudgetStrategy.ADAPTIVE_NOVELTY) {
            progress.configureGroupBudget(
                    totalRunBudget, minimumRunsPerGroup, maximumRunsPerGroup, allocationBatchSize);
        }

        CampaignSummaryWriter summaryWriter = new CampaignSummaryWriter(effectiveReportsDirectory);

        CampaignCheckpoint checkpoint = new CampaignCheckpoint(progress, summaryWriter);
        Thread shutdownCheckpoint = new Thread(checkpoint::cancel, "consistency-sweep-summary-checkpoint");
        Runtime.getRuntime().addShutdownHook(shutdownCheckpoint);

        try {
            checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);
            driver.init();

            List<FunctionalityCatalog> catalogs = selectCatalogs(getCatalogs(driver), groupSelectors);
            if (groupBudgetStrategy == GroupBudgetStrategy.ADAPTIVE_NOVELTY) {
                log.info("Campaign over {}: {} selected catalog(s), {} total adaptive run(s), "
                        + "range {}..{} per group, batch {}, master seed {}, schedule strategy {}",
                        springAppClass.getSimpleName(), catalogs.size(), totalRunBudget,
                        minimumRunsPerGroup, maximumRunsPerGroup, allocationBatchSize,
                        masterSeed, scheduleExplorationStrategy.propertyValue());
            } else {
                log.info("Campaign over {}: {} selected catalog(s), {} iteration(s) per group, "
                        + "master seed {}, schedule strategy {}",
                        springAppClass.getSimpleName(), catalogs.size(), iterationsPerGroup,
                        masterSeed, scheduleExplorationStrategy.propertyValue());
            }

            List<PlannedCatalog> plans = selectPlannedGroups(
                    catalogs.stream().map(catalog -> planCatalog(driver, catalog)).toList(), groupSelectors);
            progress.setPlanHash(planHashOf(plans));
            // Register the complete plan before execution so checkpoints show campaign-wide totals, including future catalogs.
            plans.forEach(plan -> progress.registerCatalog(
                    plan.catalog().name(), plan.functionalitiesProfiled(),
                    plan.possiblePairs(), plan.groups().size()));
            int plannedGroups = plans.stream().mapToInt(plan -> plan.groups().size()).sum();

            if (groupBudgetStrategy == GroupBudgetStrategy.ADAPTIVE_NOVELTY) {
                validateAdaptiveBudget(plannedGroups);
                progress.configureGroupBudget(
                        totalRunBudget, minimumRunsPerGroup, maximumRunsPerGroup, allocationBatchSize);
            } else {
                int fixedRunBudget = Math.toIntExact((long) plannedGroups * iterationsPerGroup);
                // Fixed budgeting is represented as min=max=batch=iterationsPerGroup.
                progress.configureGroupBudget(
                        fixedRunBudget, iterationsPerGroup, iterationsPerGroup, iterationsPerGroup);
            }

            checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);

            // Fault injection starts only after every catalog has been profiled and
            // planned. A fault-injected campaign therefore explores the same plan a normal
            // campaign would produce for this catalog/configuration/seed as intended.
            driver.setIgnoredSemanticLocks(ignoredSemanticLocks);

            if (groupBudgetStrategy == GroupBudgetStrategy.ADAPTIVE_NOVELTY) {
                exploreAdaptiveBudget(driver, plans, progress, checkpoint);
            } else {
                for (PlannedCatalog plan : plans) {
                    exploreCatalogFixedBudget(driver, plan, progress, checkpoint);
                }
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

    /** Executes fixed per-group runs, checkpointing after each fully explored group. */
    private void exploreCatalogFixedBudget(
            TestDriver driver, PlannedCatalog plan, CampaignProgress progress, CampaignCheckpoint checkpoint) {

        FunctionalityCatalog catalog = plan.catalog();
        List<FunctionalityGroup> groups = plan.groups();

        OrchestrationReport catalogCheckpoint = checkpoint.write(
                OrchestrationReport.CampaignStatus.RUNNING, null);
        log.info("{}", CampaignProgressDisplay.format(catalogCheckpoint));

        // TODO progress tracking: plan every catalog before exploration so a
        // partial summary can show campaign-wide, rather than discovered-so-far,
        // totals (relevant for multi-catalog campaigns).
        for (FunctionalityGroup group : groups) {
            long allocationStartedAt = System.currentTimeMillis();
            List<TestResult> results = driver.exploreGroup(catalog, group, progress::recordCompletedRun);
            long allocationDurationMillis = System.currentTimeMillis() - allocationStartedAt;

            List<OrchestrationReport.Finding> groupFindings = findingsOf(catalog, group, results);
            OrchestrationReport.GroupSummary groupSummary = summaryOf(group, results, groupFindings.size());
            AdaptiveGroupBudgetAllocator.BatchFeedback feedback = new GroupBudgetEvidence().observe(results);
            progress.recordBudgetAllocation(
                    "FIXED", catalog.name(), group.label(), results.size(),
                    allocationDurationMillis, 0.0, feedback);
            progress.recordCompletedGroup(catalog.name(), groupSummary, groupFindings);
            OrchestrationReport summary = checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);

            BehavioralCoverage coverage = Objects.requireNonNull(groupSummary.behavioralCoverage());
            // Convert fraction to percentage and round to one decimal place for log output.
            double discoveryPercentage = Math.round(coverage.discoveryRate() * 1_000.0) / 10.0;
            log.info("Catalog '{}', group '{}': {} run(s), {} finding(s), {} unique behavior(s) ({}% discovery), "
                    + "{} feature(s) discovered by {} run(s)",
                    catalog.name(), group.label(), results.size(), groupFindings.size(),
                    coverage.uniqueBehaviors(), discoveryPercentage,
                    coverage.uniqueFeatures(), coverage.runsAddingFeatures());
            log.info("{}", CampaignProgressDisplay.format(summary));
        }
    }

    /** Allocates one campaign-wide budget across all catalogs and groups. */
    private void exploreAdaptiveBudget(
            TestDriver driver,
            List<PlannedCatalog> plans,
            CampaignProgress progress,
            CampaignCheckpoint checkpoint) {

        Map<AdaptiveGroupBudgetAllocator.GroupKey, AdaptiveGroupExecution> executions = new LinkedHashMap<>();
        for (PlannedCatalog plan : plans) {
            for (FunctionalityGroup group : plan.groups()) {
                AdaptiveGroupBudgetAllocator.GroupKey key = new AdaptiveGroupBudgetAllocator.GroupKey(
                        plan.catalog().name(), group.label());
                executions.put(key, new AdaptiveGroupExecution(
                        plan.catalog(), group, driver.startGroupExploration(plan.catalog(), group)));
            }
        }

        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.copyOf(executions.keySet()), totalRunBudget, minimumRunsPerGroup,
                maximumRunsPerGroup, allocationBatchSize,
                masterSeed ^ ADAPTIVE_BUDGET_SEED_SALT);

        while (allocator.hasNext()) {
            AdaptiveGroupBudgetAllocator.Allocation allocation = allocator.nextAllocation();
            AdaptiveGroupExecution execution = executions.get(allocation.group());
            int firstRunIndexWithinGroup = execution.results.size();

            long allocationStartedAt = System.currentTimeMillis();
            List<TestResult> batch = execution.session.runBatch(
                    allocation.requestedRuns(), progress::recordCompletedRun);
            long allocationDurationMillis = System.currentTimeMillis() - allocationStartedAt;

            AdaptiveGroupBudgetAllocator.BatchFeedback feedback = execution.evidence.observe(batch);
            allocator.observe(allocation, feedback);

            execution.results.addAll(batch);
            List<OrchestrationReport.Finding> newFindings = findingsOf(
                    execution.catalog, execution.group, batch, firstRunIndexWithinGroup);
            execution.findingCount += newFindings.size();

            progress.recordBudgetAllocation(
                    allocation.phase().name(), execution.catalog.name(), execution.group.label(),
                    allocation.requestedRuns(), allocationDurationMillis,
                    allocation.priorityScore(), feedback);
            progress.recordCompletedGroup(
                    execution.catalog.name(),
                    summaryOf(execution.group, execution.results, execution.findingCount),
                    newFindings);

            OrchestrationReport summary = checkpoint.write(
                    OrchestrationReport.CampaignStatus.RUNNING, null);
            log.info("Adaptive allocation {}: catalog '{}', group '{}', {} run(s), reward {}, "
                    + "{} new behavior(s), {} feature-producing run(s), {} new finding family/families",
                    allocation.phase(), execution.catalog.name(), execution.group.label(),
                    batch.size(), feedback.reward(), feedback.newBehaviors(),
                    feedback.runsAddingFeatures(), feedback.newFindingFamilies());
            log.info("{}", CampaignProgressDisplay.format(summary));
        }

        // Summarize all adaptive executions again at the end, but together.
        executions.values().forEach(execution -> {
            BehavioralCoverage coverage = BehavioralCoverage.from(execution.results);
            log.info("Adaptive result: catalog '{}', group '{}': {} run(s), {} finding(s), "
                    + "{} unique behavior(s), {} unique finding family/families",
                    execution.catalog.name(), execution.group.label(), execution.results.size(),
                    execution.findingCount, coverage.uniqueBehaviors(),
                    execution.evidence.uniqueFindingFamilies());
        });
    }

    private void validateAdaptiveBudget(int plannedGroups) {
        if (plannedGroups < 1) {
            throw new IllegalArgumentException("Adaptive group budgeting requires at least one planned group");
        }
        long minimumBudget = (long) plannedGroups * minimumRunsPerGroup;
        long maximumBudget = (long) plannedGroups * maximumRunsPerGroup;
        if (totalRunBudget < minimumBudget || totalRunBudget > maximumBudget) {
            throw new IllegalArgumentException(
                    "Adaptive total run budget must be between %d and %d for %d selected group(s), got %d"
                            .formatted(minimumBudget, maximumBudget, plannedGroups, totalRunBudget));
        }
    }

    private List<OrchestrationReport.Finding> findingsOf(
            FunctionalityCatalog catalog, FunctionalityGroup group, List<TestResult> results) {

        return findingsOf(catalog, group, results, 0);
    }

    private List<OrchestrationReport.Finding> findingsOf(
            FunctionalityCatalog catalog,
            FunctionalityGroup group,
            List<TestResult> results,
            int firstRunIndexWithinGroup) {

        List<OrchestrationReport.Finding> findings = new ArrayList<>();
        for (int batchIndex = 0; batchIndex < results.size(); batchIndex++) {
            int runIndex = firstRunIndexWithinGroup + batchIndex;
            TestResult result = results.get(batchIndex);
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
                groupSelectors,
                scheduleExplorationStrategy,
                groupBudgetStrategy,
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
            Set<GroupSelector> selectedGroups,
            ScheduleExplorationStrategy strategy,
            GroupBudgetStrategy groupBudgetStrategy,
            String reportsDirectoryProperty,
            Instant startedAt,
            UUID runId) {

        if (reportsDirectoryProperty != null && !reportsDirectoryProperty.isBlank()) {
            return Path.of(reportsDirectoryProperty);
        }
        if (!isExperiment(ignoredLocks, selectedGroups, strategy, groupBudgetStrategy)
                || !requestedDirectory.equals(DEFAULT_REPORTS_DIRECTORY)) {
            return requestedDirectory;
        }
        return ExperimentReportsDirectory.pathForExperiment(startedAt, runId);
    }

    private static boolean isExperiment(
            Set<SemanticLockId> ignoredLocks,
            Set<GroupSelector> selectedGroups,
            ScheduleExplorationStrategy strategy,
            GroupBudgetStrategy groupBudgetStrategy) {

        // TODO if FIXED_PER_GROUP and RANDOM_CONSTRAINTS stop being the defaults, change this
        return !ignoredLocks.isEmpty() || !selectedGroups.isEmpty()
                || strategy != ScheduleExplorationStrategy.RANDOM_CONSTRAINTS
                || groupBudgetStrategy != GroupBudgetStrategy.FIXED_PER_GROUP;
    }

    private List<String> ignoredSemanticLockSelectors() {
        return ignoredSemanticLocks.stream().map(SemanticLockId::toSelector).sorted().toList();
    }

    List<String> configuredGroupSelectors() {
        return groupSelectors.stream().map(GroupSelector::toSelector).sorted().toList();
    }

    static List<FunctionalityCatalog> selectCatalogs(
            List<FunctionalityCatalog> catalogs, Set<GroupSelector> selectors) {

        if (selectors.isEmpty()) {
            return List.copyOf(catalogs);
        }

        Set<String> requestedCatalogs = selectors.stream()
                .map(GroupSelector::catalog)
                .collect(Collectors.toSet());
        Set<String> availableCatalogs = catalogs.stream()
                .map(FunctionalityCatalog::name)
                .collect(Collectors.toSet());
        List<String> unknownCatalogs = requestedCatalogs.stream()
                .filter(catalog -> !availableCatalogs.contains(catalog))
                .sorted()
                .toList();
        if (!unknownCatalogs.isEmpty()) {
            throw new IllegalArgumentException("Unknown catalog(s) in group selectors: " + unknownCatalogs);
        }

        return catalogs.stream().filter(catalog -> requestedCatalogs.contains(catalog.name())).toList();
    }

    static List<PlannedCatalog> selectPlannedGroups(List<PlannedCatalog> plans, Set<GroupSelector> selectors) {
        if (selectors.isEmpty()) {
            return List.copyOf(plans);
        }

        Map<GroupSelector, Integer> matchCounts = initialMatchCounts(selectors);

        List<PlannedCatalog> selectedPlans = new ArrayList<>();
        for (PlannedCatalog plan : plans) {
            List<FunctionalityGroup> selectedGroups = selectedGroupsIn(plan, selectors, matchCounts);
            if (!selectedGroups.isEmpty()) {
                selectedPlans.add(new PlannedCatalog(
                        plan.catalog(), plan.functionalitiesProfiled(), plan.possiblePairs(), selectedGroups));
            }
        }

        validateGroupSelectorMatches(matchCounts);
        return List.copyOf(selectedPlans);
    }

    /** Sort selectors before insertion so validation messages are deterministic. */
    private static Map<GroupSelector, Integer> initialMatchCounts(Set<GroupSelector> selectors) {
        Map<GroupSelector, Integer> matchCounts = new LinkedHashMap<>();
        selectors.stream()
                .sorted(Comparator.comparing(GroupSelector::toSelector))
                .forEach(selector -> matchCounts.put(selector, 0));
        return matchCounts;
    }

    private static List<FunctionalityGroup> selectedGroupsIn(
            PlannedCatalog plan, Set<GroupSelector> selectors, Map<GroupSelector, Integer> matchCounts) {

        List<FunctionalityGroup> selectedGroups = plan.groups().stream()
                .filter(group -> selectors.contains(new GroupSelector(plan.catalog().name(), group.label())))
                .toList();

        for (FunctionalityGroup group : selectedGroups) {
            GroupSelector matched = new GroupSelector(plan.catalog().name(), group.label());
            matchCounts.computeIfPresent(matched, (selector, count) -> count + 1);
        }
        return selectedGroups;
    }

    private static void validateGroupSelectorMatches(Map<GroupSelector, Integer> matchCounts) {
        List<String> unmatched = matchCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == 0)
                .map(entry -> entry.getKey().toSelector())
                .toList();
        if (!unmatched.isEmpty()) {
            throw new IllegalArgumentException("No planned group matched selector(s): " + unmatched);
        }

        // Defensive guard: planned groups are currently unique, but this protects
        // the exact-selector contract if planning later produces duplicates.
        List<String> ambiguous = matchCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> entry.getKey().toSelector())
                .toList();
        if (!ambiguous.isEmpty()) {
            throw new IllegalStateException("Group selector(s) matched multiple planned groups: " + ambiguous);
        }
    }

    private static String planHashOf(List<PlannedCatalog> plans) {
        String canonicalPlan = plans.stream()
                .map(PlannedCatalog::canonicalForm)
                .collect(Collectors.joining("\n"));
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

    private static int requiredPositiveIntegerProperty(String property) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "System property '%s' is required to be a positive integer".formatted(property));
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "System property '%s' must be a positive integer, got '%s'".formatted(property, value), e);
        }
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
                results.size(), findingCount, Map.copyOf(anomalyCounts), BehavioralCoverage.from(results));
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

    record PlannedCatalog(
            FunctionalityCatalog catalog,
            int functionalitiesProfiled,
            int possiblePairs,
            List<FunctionalityGroup> groups) {

        PlannedCatalog {
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

    private static final class AdaptiveGroupExecution {
        private final FunctionalityCatalog catalog;
        private final FunctionalityGroup group;
        private final TestDriver.GroupExplorationSession session;
        private final GroupBudgetEvidence evidence = new GroupBudgetEvidence();
        private final List<TestResult> results = new ArrayList<>();
        private int findingCount;

        private AdaptiveGroupExecution(
                FunctionalityCatalog catalog,
                FunctionalityGroup group,
                TestDriver.GroupExplorationSession session) {

            this.catalog = catalog;
            this.group = group;
            this.session = session;
        }
    }

}
