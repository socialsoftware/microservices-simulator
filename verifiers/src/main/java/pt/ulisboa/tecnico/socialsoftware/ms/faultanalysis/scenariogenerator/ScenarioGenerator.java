package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.visitor.VoidVisitor;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.FaultAnalysisProperties;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.CommandHandlerVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.ServiceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.SpockTestVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor.WorkflowFunctionalityVisitor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

@Component
public class ScenarioGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioGenerator.class);

    // Predicates for file tree analysis
    private static final Predicate<Path> javaFilter = path -> !Files.isDirectory(path) && path.toString().endsWith(
            ".java");
    private static final Predicate<Path> groovyFilter = path -> !Files.isDirectory(path) && path.toString().endsWith(
            ".groovy");
    private final FaultAnalysisProperties faultAnalysisProperties;

    // File tree
    private final List<Path> javaFiles;
    private final List<Path> groovyFiles;

    private final ApplicationAnalysisContext applicationAnalysisContext = new ApplicationAnalysisContext();

    public ScenarioGenerator(FaultAnalysisProperties faultAnalysisProperties) {
        this.faultAnalysisProperties = faultAnalysisProperties;

        // Obtain all java and groovy files from the "applications/**" file tree
        this.javaFiles = walkApplicationFileTree(javaFilter);
        this.groovyFiles = walkApplicationFileTree(groovyFilter);
    }

    public void init() {
        // Pass 1: Collect services and classify method access policies (READ/WRITE)
        ServiceVisitor serviceVisitor = new ServiceVisitor();
        javaFiles.forEach(p -> visitFile(p, serviceVisitor));
        logger.info("Pass 1 complete: {} services found", applicationAnalysisContext.services.size());

        // Pass 2: Collect command handlers and map command types to dispatch info
        CommandHandlerVisitor commandHandlerVisitor = new CommandHandlerVisitor();
        javaFiles.forEach(p -> visitFile(p, commandHandlerVisitor));
        logger.info("Pass 2 complete: {} command handlers found", applicationAnalysisContext.commandHandlers.size());

        // Pass 3: Collect saga workflow functionalities and link steps to StepFootprints
        WorkflowFunctionalityVisitor workflowVisitor = new WorkflowFunctionalityVisitor();
        javaFiles.forEach(p -> visitFile(p, workflowVisitor));
        logger.info("Pass 3 complete: {} sagas, {} steps found",
                applicationAnalysisContext.sagas.size(), applicationAnalysisContext.steps.size());

        // Pass 4: Parse Groovy/Spock test files to extract test inputs for scenario generation
        SpockTestVisitor spockVisitor = new SpockTestVisitor();
        groovyFiles.forEach(p -> visitGroovyFile(p, spockVisitor));
        logger.info("Pass 4 complete: {} Groovy files analyzed", groovyFiles.size());
    }

    public ApplicationAnalysisContext getApplicationAnalysisContext() {
        return applicationAnalysisContext;
    }

    public List<Path> getJavaFiles() {
        return javaFiles;
    }

    public List<Path> getGroovyFiles() {
        return groovyFiles;
    }

    private void visitFile(Path p, VoidVisitor<ApplicationAnalysisContext> visitor) {
        try {
            visitor.visit(StaticJavaParser.parse(p), applicationAnalysisContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void visitGroovyFile(Path p, SpockTestVisitor visitor) {
        try {
            CompilerConfiguration config = new CompilerConfiguration();
            SourceUnit su = SourceUnit.create(p.getFileName().toString(), Files.readString(p), config.getTolerance());
            su.parse();
            su.completePhase(); // phase must be marked complete before convert() can proceed
            su.convert();
            ModuleNode module = su.getAST();
            module.getClasses().forEach(visitor::visitClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Path> walkApplicationFileTree(Predicate<Path> filter) {
        List<Path> fileList = new ArrayList<>();
        Path targetApplicationsDir = Paths.get(faultAnalysisProperties.getTargetApplicationsDir());
        try {
            Files.walkFileTree(targetApplicationsDir, EnumSet.noneOf(FileVisitOption.class), 1000,
                    new SimpleFileVisitor<>() {
                        @Override
                        @NonNull
                        public FileVisitResult visitFile(@NonNull Path path, @NonNull BasicFileAttributes attrs) {
                            if (filter.test(path)) {
                                fileList.add(path);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            logger.error("{} {}", e.getClass().getName(), e.getMessage());
        }
        return fileList;
    }
}
