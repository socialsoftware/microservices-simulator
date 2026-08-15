package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityFootprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroup;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityGroupPlanner;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;

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

    /** Name of the campaign-level summary written at the reports root. */
    private static final String SUMMARY_FILE_NAME = "campaign-summary.json";

    private static final String RUN_REPORT_FILE_NAME = "test-report-%05d.json";

    private final Class<?> springAppClass;

    private List<String> springAppArgs = List.of();
    private int iterationsPerGroup = DEFAULT_ITERATIONS_PER_GROUP;
    private long masterSeed = DEFAULT_MASTER_SEED;
    private Path reportsDirectory = DEFAULT_REPORTS_DIRECTORY;

    private Orchestrator(Class<?> springAppClass) {
        this.springAppClass = springAppClass;
    }

    public static Orchestrator of(Class<?> springAppClass) {
        return new Orchestrator(springAppClass);
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

    /** Where per-run reports and the campaign summary are written. */
    public Orchestrator withReportsDirectory(Path reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
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

        TestDriver driver = new TestDriver(springAppClass, springAppArgs, reportsDirectory)
                .setIterations(iterationsPerGroup)
                .setMasterSeed(masterSeed);

        List<OrchestrationReport.CatalogSummary> catalogSummaries = new ArrayList<>();
        List<OrchestrationReport.Finding> findings = new ArrayList<>();

        driver.init();
        try {
            List<FunctionalityCatalog> catalogs = getCatalogs(driver);
            log.info("Campaign over {}: {} catalog(s), {} iteration(s) per group, master seed {}",
                    springAppClass.getSimpleName(), catalogs.size(), iterationsPerGroup, masterSeed);

            for (FunctionalityCatalog catalog : catalogs) {
                catalogSummaries.add(exploreCatalog(driver, catalog, findings));
            }
        } finally {
            driver.shutdown();
        }

        OrchestrationReport report = new OrchestrationReport(
                springAppClass.getName(),
                masterSeed,
                iterationsPerGroup,
                StringUtils.toPortableString(reportsDirectory),
                System.currentTimeMillis() - startedAt,
                List.copyOf(catalogSummaries),
                List.copyOf(findings));

        writeSummary(report);
        log.info("Campaign finished:{}{}", System.lineSeparator(), report.summary());
        return report;
    }

    /**
     * Profiles, plans and explores one catalog, appending its findings to
     * {@code findings}.
     */
    private OrchestrationReport.CatalogSummary exploreCatalog(
            TestDriver driver, FunctionalityCatalog catalog, List<OrchestrationReport.Finding> findings) {

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

        List<OrchestrationReport.GroupSummary> groupSummaries = new ArrayList<>();
        int runsExecuted = 0;
        int catalogFindings = 0;

        for (FunctionalityGroup group : groups) {
            List<TestResult> results = driver.exploreGroup(catalog, group);

            List<OrchestrationReport.Finding> groupFindings = findingsOf(catalog, group, results);
            findings.addAll(groupFindings);

            runsExecuted += results.size();
            catalogFindings += groupFindings.size();
            groupSummaries.add(summaryOf(group, results, groupFindings.size()));

            log.info("Catalog '{}', group '{}': {} run(s), {} finding(s)",
                    catalog.name(), group.label(), results.size(), groupFindings.size());
        }

        return new OrchestrationReport.CatalogSummary(
                catalog.name(),
                footprints.size(),
                possiblePairs,
                groups.size(),
                0, // every planned group is explored under the fixed-budget policy
                runsExecuted,
                catalogFindings,
                List.copyOf(groupSummaries));
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

    private void writeSummary(OrchestrationReport report) {
        Path target = reportsDirectory.resolve(SUMMARY_FILE_NAME);
        try {
            new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(target.toFile(), report);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write campaign summary to " + target, e);
        }
    }
}
