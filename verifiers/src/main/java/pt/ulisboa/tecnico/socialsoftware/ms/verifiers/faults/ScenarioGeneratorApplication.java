package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.report.AnalysisHtmlReportRenderer;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogJsonlWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@SpringBootApplication
public class ScenarioGeneratorApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioGeneratorApplication.class);
    private static final DateTimeFormatter RUN_DIRECTORY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final String applicationsRoot;

    private final String applicationBaseDir;
    private final String outputRoot;
    private final String reportHtmlPath;
    private final boolean scenarioCatalogEnabled;
    private final String scenarioCatalogPath;
    private final String scenarioCatalogManifestPath;
    private final String scenarioCatalogRejectedInputsPath;
    private final boolean scenarioCatalogIncludeSingles;
    private final int scenarioCatalogMaxSagaSetSize;
    private final int scenarioCatalogMaxScenarios;
    private final int scenarioCatalogMaxInputVariantsPerSaga;
    private final int scenarioCatalogMaxSchedulesPerInputTuple;
    private final boolean scenarioCatalogAllowTypeOnlyFallback;
    private final String scenarioCatalogInputPolicy;
    private final String scenarioCatalogScheduleStrategy;
    private final long scenarioCatalogDeterministicSeed;

    private final Path applicationsRootPath;
    private final Path applicationPath;
    private final Path outputRootPath;
    private Path runOutputDirectory;

    public ScenarioGeneratorApplication(
            @Value("${verifiers.applications-root}") String applicationsRoot,
            @Value("${verifiers.application-base-dir}") String applicationBaseDir,
            @Value("${verifiers.output-root:output}") String outputRoot,
            @Value("${verifiers.report-html-path:}") String reportHtmlPath,
            @Value("${verifiers.scenario-catalog.enabled:false}") boolean scenarioCatalogEnabled,
            @Value("${verifiers.scenario-catalog.catalog-path:scenario-catalog.jsonl}") String scenarioCatalogPath,
            @Value("${verifiers.scenario-catalog.manifest-path:scenario-catalog-manifest.json}") String scenarioCatalogManifestPath,
            @Value("${verifiers.scenario-catalog.rejected-inputs-path:scenario-catalog-rejected-inputs.jsonl}") String scenarioCatalogRejectedInputsPath,
            @Value("${verifiers.scenario-catalog.include-singles:true}") boolean scenarioCatalogIncludeSingles,
            @Value("${verifiers.scenario-catalog.max-saga-set-size:1}") int scenarioCatalogMaxSagaSetSize,
            @Value("${verifiers.scenario-catalog.max-scenarios:100}") int scenarioCatalogMaxScenarios,
            @Value("${verifiers.scenario-catalog.max-input-variants-per-saga:3}") int scenarioCatalogMaxInputVariantsPerSaga,
            @Value("${verifiers.scenario-catalog.max-schedules-per-input-tuple:20}") int scenarioCatalogMaxSchedulesPerInputTuple,
            @Value("${verifiers.scenario-catalog.allow-type-only-fallback:false}") boolean scenarioCatalogAllowTypeOnlyFallback,
            @Value("${verifiers.scenario-catalog.input-policy:RESOLVED_OR_REPLAYABLE}") String scenarioCatalogInputPolicy,
            @Value("${verifiers.scenario-catalog.schedule-strategy:SERIAL}") String scenarioCatalogScheduleStrategy,
            @Value("${verifiers.scenario-catalog.deterministic-seed:1234}") long scenarioCatalogDeterministicSeed
    ) {
        this.applicationsRoot = Objects.requireNonNull(applicationsRoot, "applicationsRoot cannot be null");
        this.applicationBaseDir = Objects.requireNonNull(applicationBaseDir, "applicationBaseDir cannot be null");
        this.outputRoot = Objects.requireNonNull(outputRoot, "outputRoot cannot be null");
        this.reportHtmlPath = Objects.requireNonNull(reportHtmlPath, "reportHtmlPath cannot be null");
        this.scenarioCatalogEnabled = scenarioCatalogEnabled;
        this.scenarioCatalogPath = Objects.requireNonNull(scenarioCatalogPath, "scenarioCatalogPath cannot be null");
        this.scenarioCatalogManifestPath = Objects.requireNonNull(scenarioCatalogManifestPath, "scenarioCatalogManifestPath cannot be null");
        this.scenarioCatalogRejectedInputsPath = Objects.requireNonNull(scenarioCatalogRejectedInputsPath, "scenarioCatalogRejectedInputsPath cannot be null");
        this.scenarioCatalogIncludeSingles = scenarioCatalogIncludeSingles;
        this.scenarioCatalogMaxSagaSetSize = scenarioCatalogMaxSagaSetSize;
        this.scenarioCatalogMaxScenarios = scenarioCatalogMaxScenarios;
        this.scenarioCatalogMaxInputVariantsPerSaga = scenarioCatalogMaxInputVariantsPerSaga;
        this.scenarioCatalogMaxSchedulesPerInputTuple = scenarioCatalogMaxSchedulesPerInputTuple;
        this.scenarioCatalogAllowTypeOnlyFallback = scenarioCatalogAllowTypeOnlyFallback;
        this.scenarioCatalogInputPolicy = Objects.requireNonNull(scenarioCatalogInputPolicy, "scenarioCatalogInputPolicy cannot be null");
        this.scenarioCatalogScheduleStrategy = Objects.requireNonNull(scenarioCatalogScheduleStrategy, "scenarioCatalogScheduleStrategy cannot be null");
        this.scenarioCatalogDeterministicSeed = scenarioCatalogDeterministicSeed;
        this.applicationsRootPath = Path.of(this.applicationsRoot).toAbsolutePath().normalize();
        this.applicationPath = this.applicationsRootPath.resolve(this.applicationBaseDir).normalize();
        this.outputRootPath = Path.of(this.outputRoot).toAbsolutePath().normalize();
    }

    private void configureSymbolSolver() {
        // Set correct Java version
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

        CombinedTypeSolver solver = new CombinedTypeSolver();
        // simulator classes are in fat jar -> ReflectionTypeSolver can see them
        solver.add(new ReflectionTypeSolver());
        solver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));

        // Only add source roots for the configured application subtree.
        try (var stream = Files.find(
                applicationPath,
                8,
                (p, a) -> a.isDirectory() && p.endsWith(Paths.get("src", "main", "java"))
        )) {
            stream.forEach(srcRoot -> {
                solver.add(new JavaParserTypeSolver(srcRoot));
                logger.info("Added source root: {}", srcRoot);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure source roots under " + applicationPath, e);
        }

        config.setSymbolResolver(new JavaSymbolSolver(solver));
        StaticJavaParser.setConfiguration(config);
    }

    public static void main(String[] args) {
        SpringApplication.run(ScenarioGeneratorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("STARTING FAULT ANALYSIS MODULE");

        if (!Files.isDirectory(applicationPath)) {
            throw new IllegalArgumentException("Configured application base dir does not exist: " + applicationPath);
        }

        // Configure the JavaParser symbol solver
        configureSymbolSolver();

        ApplicationsFileTreeParser parser = new ApplicationsFileTreeParser();
        parser.parse(applicationPath);
        logger.info("Java source files: {}", parser.getJavaFilePaths().size());
        logger.info("Groovy test files: {}", parser.getGroovyFilePaths().size());

        ApplicationAnalysisState applicationAnalysisState = new ApplicationAnalysisState();

        // Phase 1 — collect command-handler dispatch target FQNs (concrete service types only)
        CommandHandlerIndexVisitor commandHandlerIndexVisitor = new CommandHandlerIndexVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                commandHandlerIndexVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Phase 2 — classify domain services (only command-handler dispatch targets are admitted)
        ServiceVisitor serviceVisitor = new ServiceVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                serviceVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Phase 3 — build command dispatch map
        CommandHandlerVisitor commandHandlerVisitor = new CommandHandlerVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                commandHandlerVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        WorkflowFunctionalityVisitor workflowFunctionalityVisitor = new WorkflowFunctionalityVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                workflowFunctionalityVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        WorkflowFunctionalityCreationSiteVisitor sagaCreationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                sagaCreationSiteVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        GroovySourceIndex groovySourceIndex = new GroovySourceIndex();
        Path groovyTestRoot = applicationPath.resolve(Paths.get("src", "test", "groovy")).normalize();
        if (Files.isDirectory(groovyTestRoot)) {
            try {
                groovySourceIndex.parse(groovyTestRoot);
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse Groovy test sources under " + groovyTestRoot, e);
            }
        } else {
            logger.info("No Groovy test root found under {}; skipping Groovy trace parsing", groovyTestRoot);
        }

        GroovyConstructorInputTraceVisitor groovyTraceVisitor = new GroovyConstructorInputTraceVisitor();
        groovyTraceVisitor.visit(groovySourceIndex, applicationAnalysisState);

        String textReport = applicationAnalysisState.formatHumanReadableReport();
        logger.info("Analysis report:\n{}", textReport);

        OffsetDateTime generatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        AnalysisHtmlReportRenderer htmlReportRenderer = new AnalysisHtmlReportRenderer();
        String htmlReport = htmlReportRenderer.render(
                applicationAnalysisState,
                new AnalysisHtmlReportRenderer.ReportMetadata(
                        applicationsRoot,
                        applicationBaseDir,
                        generatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                ),
                textReport
        );

        runOutputDirectory = createRunOutputDirectory(generatedAt);
        logger.info("Verifier run output directory: {}", runOutputDirectory.toAbsolutePath().normalize());

        Path htmlOutputPath = resolveHtmlReportPath();
        Path parent = htmlOutputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(htmlOutputPath, htmlReport);
        logger.info("Analysis HTML report written to {}", htmlOutputPath.toAbsolutePath().normalize());

        runScenarioCatalogExport(applicationAnalysisState, generatedAt);
    }

    private void runScenarioCatalogExport(ApplicationAnalysisState applicationAnalysisState,
                                          OffsetDateTime generatedAt) throws IOException {
        if (!scenarioCatalogEnabled) {
            return;
        }

        ApplicationAnalysisScenarioModelAdapter adapter = new ApplicationAnalysisScenarioModelAdapter();
        ScenarioModelAdapterResult adapterResult = adapter.adapt(applicationAnalysisState);

        ScenarioGeneratorConfig scenarioGeneratorConfig = new ScenarioGeneratorConfig(
                true,
                scenarioCatalogIncludeSingles,
                scenarioCatalogMaxSagaSetSize,
                scenarioCatalogMaxScenarios,
                scenarioCatalogMaxInputVariantsPerSaga,
                scenarioCatalogMaxSchedulesPerInputTuple,
                scenarioCatalogAllowTypeOnlyFallback,
                parseInputPolicy(scenarioCatalogInputPolicy),
                parseScheduleStrategy(scenarioCatalogScheduleStrategy),
                scenarioCatalogDeterministicSeed
        );

        var generationResult = ScenarioGenerator.generate(
                adapterResult.sagaDefinitions(),
                adapterResult.inputVariants(),
                scenarioGeneratorConfig);

        ScenarioGenerationResult exportResult = new ScenarioGenerationResult(
                generationResult.schemaVersion(),
                generationResult.effectiveConfig(),
                generationResult.scenarioPlans(),
                generationResult.rejectedInputVariants(),
                mergeCounts(adapterResult.counts(), generationResult.counts()),
                mergeWarnings(adapterResult.diagnostics(), generationResult.warnings()));

        Path catalogOutputPath = resolveScenarioCatalogPath();
        Path manifestOutputPath = resolveScenarioCatalogManifestPath();
        Path rejectedInputsOutputPath = resolveScenarioCatalogRejectedInputsPath();
        var manifest = new ScenarioCatalogJsonlWriter().write(
                exportResult,
                catalogOutputPath,
                manifestOutputPath,
                rejectedInputsOutputPath,
                generatedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        logger.info(
                "Scenario catalog export wrote {} scenarios to {}, rejected inputs {}, and manifest {}",
                manifest.counts().getOrDefault("scenariosExported", 0),
                catalogOutputPath.toAbsolutePath().normalize(),
                rejectedInputsOutputPath.toAbsolutePath().normalize(),
                manifestOutputPath.toAbsolutePath().normalize());
    }

    private Path resolveHtmlReportPath() {
        return resolveRunRelativePath(reportHtmlPath, "analysis-report.html");
    }

    private Path resolveScenarioCatalogPath() {
        return resolveRunRelativePath(scenarioCatalogPath, "scenario-catalog.jsonl");
    }

    private Path resolveScenarioCatalogManifestPath() {
        return resolveRunRelativePath(scenarioCatalogManifestPath, "scenario-catalog-manifest.json");
    }

    private Path resolveScenarioCatalogRejectedInputsPath() {
        return resolveRunRelativePath(scenarioCatalogRejectedInputsPath, "scenario-catalog-rejected-inputs.jsonl");
    }

    private Path resolveRunRelativePath(String configuredPath, String defaultFileName) {
        Path baseDirectory = requireRunOutputDirectory();
        if (configuredPath == null || configuredPath.isBlank()) {
            return baseDirectory.resolve(defaultFileName).normalize();
        }

        Path configured = Path.of(configuredPath);
        if (configured.isAbsolute()) {
            throw new IllegalArgumentException("Configured output path must be relative to the verifier run output directory: " + configuredPath);
        }

        Path resolved = baseDirectory.resolve(configured).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException(
                    "Configured relative output path must stay under verifier run output directory: " + configuredPath);
        }
        rejectExistingSymlinkSegments(baseDirectory, resolved, configuredPath);
        return resolved;
    }

    private void rejectExistingSymlinkSegments(Path baseDirectory, Path resolved, String configuredPath) {
        Path relative = baseDirectory.relativize(resolved);
        Path current = baseDirectory;
        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException(
                        "Configured output path must not traverse symbolic links under verifier run output directory: " + configuredPath);
            }
        }
    }

    private Path requireRunOutputDirectory() {
        if (runOutputDirectory == null) {
            throw new IllegalStateException("Run output directory has not been initialized");
        }
        return runOutputDirectory;
    }

    private Path createRunOutputDirectory(OffsetDateTime generatedAt) throws IOException {
        Files.createDirectories(outputRootPath);
        String timestamp = generatedAt.format(RUN_DIRECTORY_TIMESTAMP_FORMATTER);
        String runDirectoryName = sanitizeRunDirectoryName(applicationBaseDir) + "-" + timestamp;
        Path candidate = outputRootPath.resolve(runDirectoryName).normalize();
        if (!candidate.startsWith(outputRootPath)) {
            throw new IllegalArgumentException("Resolved run output directory must stay under verifier output root: " + candidate);
        }

        int suffix = 2;
        while (Files.exists(candidate)) {
            candidate = outputRootPath.resolve(runDirectoryName + "-" + suffix).normalize();
            suffix++;
        }
        Files.createDirectories(candidate);
        return candidate;
    }

    private static String sanitizeRunDirectoryName(String applicationBaseDir) {
        String appName = Path.of(applicationBaseDir).getFileName() == null
                ? applicationBaseDir
                : Path.of(applicationBaseDir).getFileName().toString();
        String sanitized = appName.replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return sanitized.isBlank() ? "application" : sanitized;
    }

    private static ScenarioGeneratorConfig.InputPolicy parseInputPolicy(String value) {
        if (value == null || value.isBlank()) {
            return ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE;
        }

        return ScenarioGeneratorConfig.InputPolicy.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    private static ScenarioGeneratorConfig.ScheduleStrategy parseScheduleStrategy(String value) {
        if (value == null || value.isBlank()) {
            return ScenarioGeneratorConfig.ScheduleStrategy.SERIAL;
        }

        return ScenarioGeneratorConfig.ScheduleStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    private static LinkedHashMap<String, Integer> mergeCounts(Map<String, Integer> first, Map<String, Integer> second) {
        LinkedHashMap<String, Integer> merged = new LinkedHashMap<>();
        if (first != null) {
            first.forEach((key, value) -> merged.merge(key, value == null ? 0 : value, Integer::sum));
        }
        if (second != null) {
            second.forEach((key, value) -> merged.merge(key, value == null ? 0 : value, Integer::sum));
        }
        return merged;
    }

    private static List<String> mergeWarnings(List<String> first, List<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return List.copyOf(merged);
    }

}
