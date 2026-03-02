package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ScenarioGenerator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Profile("fault-analysis")
@EnableConfigurationProperties(FaultAnalysisProperties.class)
@SpringBootApplication(scanBasePackages = {
        "pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis",
        "pt.ulisboa.tecnico.socialsoftware.ms.exception"
})
public class FaultAnalysisApplication implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(FaultAnalysisApplication.class);
    private final ScenarioGenerator scenarioGenerator;
    private final FaultAnalysisProperties faultAnalysisProperties;

    public FaultAnalysisApplication(ScenarioGenerator scenarioGenerator, FaultAnalysisProperties faultAnalysisProperties) {
        this.scenarioGenerator = scenarioGenerator;
        this.faultAnalysisProperties = faultAnalysisProperties;
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(FaultAnalysisApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    private void configureSymbolSolver() {
        // Set correct Java version
        Path applicationsRoot = Paths.get(faultAnalysisProperties.getTargetApplicationsDir());
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

        CombinedTypeSolver solver = new CombinedTypeSolver();
        // simulator classes are in fat jar -> ReflectionTypeSolver can see them
        solver.add(new ReflectionTypeSolver());
        solver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));

        // Add all /applications/**/src/main/java
        try (var stream = Files.find(
                applicationsRoot,
                8,
                (p, a) -> a.isDirectory() && p.endsWith(Paths.get("src", "main", "java"))
        )) {
            stream.forEach(srcRoot -> {
                solver.add(new JavaParserTypeSolver(srcRoot));
                logger.info("Added source root: {}", srcRoot);
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure source roots under " + applicationsRoot, e);
        }

        config.setSymbolResolver(new JavaSymbolSolver(solver));
        StaticJavaParser.setConfiguration(config);
    }

    @Override
    public void run(String... args) {
        logger.info("STARTING FAULT ANALYSIS MODULE");

        // Configure the JavaParser symbol solver
        configureSymbolSolver();

        scenarioGenerator.init();

//        Map<String, List<String>> sagaStepsMap = scenarioGenerator.getSagaStepsMap();
//        Map<String, Set<StepFootprint>> stepFootprints = scenarioGenerator.getStepFootprints();
//        Map<String, Set<StepFootprint>> aggregateTouchIndex = scenarioGenerator.getAggregateTouchIndex();
//
//        sagaStepsMap.forEach((sagaFqn, steps) -> {
//            String simpleName = sagaFqn.substring(sagaFqn.lastIndexOf('.') + 1);
//            logger.info("Saga: {} ({} steps)", simpleName, steps.size());
//            steps.forEach(stepKey ->
//                logger.info("  {} -> {}", stepKey, stepFootprints.getOrDefault(stepKey, Set.of()))
//            );
//        });
//
//        logger.info("--- Aggregate Touch Index ---");
//        aggregateTouchIndex.forEach((aggregate, footprints) ->
//                logger.info("  {} <- {}", aggregate, footprints)
//        );

        logger.info("FINISHED FAULT ANALYSIS");
    }

}
