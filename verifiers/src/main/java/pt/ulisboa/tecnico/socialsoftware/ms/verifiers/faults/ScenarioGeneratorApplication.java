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
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@SpringBootApplication
public class ScenarioGeneratorApplication implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioGeneratorApplication.class);
    private static final DateTimeFormatter HTML_REPORT_ARCHIVE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final String applicationsRoot;

    private final String applicationBaseDir;
    private final String reportHtmlPath;

    private final Path applicationsRootPath;
    private final Path applicationPath;

    public ScenarioGeneratorApplication(
            @Value("${verifiers.applications-root}") String applicationsRoot,
            @Value("${verifiers.application-base-dir}") String applicationBaseDir,
            @Value("${verifiers.report-html-path:}") String reportHtmlPath
    ) {
        this.applicationsRoot = Objects.requireNonNull(applicationsRoot, "applicationsRoot cannot be null");
        this.applicationBaseDir = Objects.requireNonNull(applicationBaseDir, "applicationBaseDir cannot be null");
        this.reportHtmlPath = Objects.requireNonNull(reportHtmlPath, "reportHtmlPath cannot be null");
        this.applicationsRootPath = Path.of(this.applicationsRoot).toAbsolutePath().normalize();
        this.applicationPath = this.applicationsRootPath.resolve(this.applicationBaseDir).normalize();
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

        Path htmlOutputPath = resolveHtmlReportPath();
        Path parent = htmlOutputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(htmlOutputPath, htmlReport);
        Path archivedHtmlOutputPath = resolveArchivedHtmlReportPath(htmlOutputPath, generatedAt);
        Files.writeString(archivedHtmlOutputPath, htmlReport);
        logger.info("Analysis HTML report written to {}", htmlOutputPath.toAbsolutePath().normalize());
        logger.info("Analysis HTML archive written to {}", archivedHtmlOutputPath.toAbsolutePath().normalize());
    }

    private Path resolveHtmlReportPath() {
        if (reportHtmlPath == null || reportHtmlPath.isBlank()) {
            return applicationPath.resolve("analysis-report.html").normalize();
        }

        Path configured = Path.of(reportHtmlPath);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }

        return applicationPath.resolve(configured).normalize();
    }

    private Path resolveArchivedHtmlReportPath(Path htmlOutputPath, OffsetDateTime generatedAt) {
        String archiveTimestamp = generatedAt.format(HTML_REPORT_ARCHIVE_TIMESTAMP_FORMATTER);
        String archiveFileName = appendTimestampBeforeExtension(htmlOutputPath.getFileName().toString(), archiveTimestamp);
        Path parent = htmlOutputPath.getParent();
        if (parent == null) {
            return Path.of(archiveFileName).normalize();
        }

        return parent.resolve(archiveFileName).normalize();
    }

    private static String appendTimestampBeforeExtension(String fileName, String archiveTimestamp) {
        int extensionStart = fileName.lastIndexOf('.');
        if (extensionStart <= 0) {
            return fileName + "-" + archiveTimestamp;
        }

        return fileName.substring(0, extensionStart)
                + "-"
                + archiveTimestamp
                + fileName.substring(extensionStart);
    }
}
