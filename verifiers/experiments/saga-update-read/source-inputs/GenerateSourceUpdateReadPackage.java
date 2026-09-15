import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ApplicationsFileTreeParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputVariantNormalizer;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.PrerequisiteScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
/** Experiment: unchanged source visitors, source-derived setup and ordinary package generation. */
public final class GenerateSourceUpdateReadPackage {
 public static void main(String[] args) throws Exception {
  Path applicationsRootPath=Path.of(args[0]); String applicationBaseDir="quizzes";
  Path applicationPath=applicationsRootPath.resolve(applicationBaseDir);
  var solver=new CombinedTypeSolver(new ReflectionTypeSolver(),new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()),new JavaParserTypeSolver(applicationPath.resolve("src/main/java")));
  StaticJavaParser.setConfiguration(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21).setSymbolResolver(new JavaSymbolSolver(solver)));
        ApplicationsFileTreeParser parser = new ApplicationsFileTreeParser();
        parser.parse(applicationPath);
        
        

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

        EventHandlingBridgeVisitor eventHandlingBridgeVisitor = new EventHandlingBridgeVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                eventHandlingBridgeVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        eventHandlingBridgeVisitor.finish(applicationAnalysisState);

        EventConsequenceVisitor eventConsequenceVisitor = new EventConsequenceVisitor();
        parser.getJavaFilePathsForApplication(applicationsRootPath, applicationBaseDir).forEach((fqn, path) -> {
            try {
                eventConsequenceVisitor.visit(StaticJavaParser.parse(path), applicationAnalysisState);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        eventConsequenceVisitor.finish(applicationAnalysisState);

        GroovySourceIndex groovySourceIndex = new GroovySourceIndex();
        Path groovyTestRoot = applicationPath.resolve(Paths.get("src", "test", "groovy")).normalize();
        if (Files.isDirectory(groovyTestRoot)) {
            try {
                groovySourceIndex.parse(groovyTestRoot);
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse Groovy test sources under " + groovyTestRoot, e);
            }
        } else {
            
        }

        GroovyConstructorInputTraceVisitor groovyTraceVisitor = new GroovyConstructorInputTraceVisitor();
        groovyTraceVisitor.visit(groovySourceIndex, applicationAnalysisState);


  var model=new ApplicationAnalysisScenarioModelAdapter().adapt(applicationAnalysisState);

  final String sourceClass="pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.UpdateTournamentTest";
  final String sourceMethod="update tournament successfully";
  var sagas=Set.of("pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.FindTournamentFunctionalitySagas",
      "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas");
  var inputs=model.inputVariants().stream().filter(i -> sourceClass.equals(i.sourceClassFqn())
      && sourceMethod.equals(i.sourceMethodName()) && sagas.contains(i.sagaFqn())
      && i.sourceMode()==pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode.SAGAS).toList();
  if(inputs.size()!=2) throw new IllegalStateException("Expected exactly two existing successful-feature inputs, got "+inputs.size());
  var definitions=model.sagaDefinitions().stream().filter(s -> sagas.contains(s.sagaFqn())).toList();
  var cfg=new ScenarioGeneratorConfig(true, ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
    ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,false,2,100,10,20,false,
    ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,9092026L);
  var generated=pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator.generate(
      definitions,inputs,model.eventConsequenceDefinitions(),model.sourceSetupPlanBindings(),model.aggregateKeyInputEvidence(),cfg);
  if(generated.workloadPlans().size()!=6) throw new IllegalStateException("Expected six read placements; "+generated.counts()+" "+generated.warnings());
  for(var workload:generated.workloadPlans()) {
    if(workload.prerequisiteBaseline()!=null || workload.setupPlan()==null)
      throw new IllegalStateException("Every workload must have source setup and no prerequisite provider");
    if(!EagerFaultScenarioGenerator.evaluateMaterializability(workload).materializable())
      throw new IllegalStateException("Not materializable: "+EagerFaultScenarioGenerator.evaluateMaterializability(workload));
  }
  var eager=EagerFaultScenarioGenerator.generate(generated,new RecoveryScheduleCap(100));
  new ExecutableArtifactWriter().write(model,"quizzes",eager,Path.of(args[1]));
  new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1],"generation-proof.json").toFile(),
      Map.of("workloads",generated.workloadPlans().size(),"scenarios",eager.faultScenarios().size(),
      "sourceClass",sourceClass,"sourceMethod",sourceMethod,"inputs",inputs.stream().map(InputVariant::deterministicId).toList(),
      "counts",generated.counts(),"warnings",generated.warnings()));
 }
}
