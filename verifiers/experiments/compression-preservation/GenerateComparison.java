import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ApplicationsFileTreeParser;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerIndexVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ConstructorCopyVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventConsequenceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventHandlingBridgeVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ServiceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityCreationSiteVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingCalculator;
import java.math.BigInteger;
import java.util.*;
import java.io.BufferedWriter;
import java.security.MessageDigest;
public final class GenerateComparison {
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    public static void main(String[] args) throws Exception {
        Path applicationsRoot = Path.of(args[0]);
        String application = args[1];
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        Path applicationPath = applicationsRoot.resolve(application);
        var solver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()),
                new JavaParserTypeSolver(applicationsRoot.getParent().resolve("simulator/src/main/java")),
                new JavaParserTypeSolver(applicationPath.resolve("src/main/java")));
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(solver)));

        ApplicationsFileTreeParser parser = new ApplicationsFileTreeParser();
        parser.parse(applicationPath);
        ApplicationAnalysisState state = new ApplicationAnalysisState();

        CommandHandlerIndexVisitor commandHandlers = new CommandHandlerIndexVisitor();
        visitJava(parser, applicationsRoot, application, state, commandHandlers::visit);
        ServiceVisitor services = new ServiceVisitor();
        visitJava(parser, applicationsRoot, application, state, services::visit);
        CommandHandlerVisitor dispatch = new CommandHandlerVisitor();
        visitJava(parser, applicationsRoot, application, state, dispatch::visit);
        WorkflowFunctionalityVisitor workflows = new WorkflowFunctionalityVisitor();
        visitJava(parser, applicationsRoot, application, state, workflows::visit);
        WorkflowFunctionalityCreationSiteVisitor creationSites = new WorkflowFunctionalityCreationSiteVisitor();
        visitJava(parser, applicationsRoot, application, state, creationSites::visit);
        EventHandlingBridgeVisitor bridges = new EventHandlingBridgeVisitor();
        visitJava(parser, applicationsRoot, application, state, bridges::visit);
        bridges.finish(state);
        EventConsequenceVisitor consequences = new EventConsequenceVisitor();
        visitJava(parser, applicationsRoot, application, state, consequences::visit);
        consequences.finish(state);
        ConstructorCopyVisitor copies = new ConstructorCopyVisitor();
        visitJava(parser, applicationsRoot, application, state, copies::visit);
        copies.finish(state);

        Path testRoot = applicationPath.resolve("src/test/groovy");
        if (!Files.isDirectory(testRoot)) {
            throw new IllegalArgumentException("Missing Groovy test root: " + testRoot);
        }
        GroovySourceIndex sourceIndex = new GroovySourceIndex();
        sourceIndex.parse(testRoot);
        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state);

        var model = new ApplicationAnalysisScenarioModelAdapter().adapt(state);


        String prefix="pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.";
        Set<String> names=Set.of(prefix+"FindTournamentFunctionalitySagas",prefix+"UpdateTournamentFunctionalitySagas");
        String test="pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.UpdateTournamentTest";
        var inputs=model.inputVariants().stream().filter(i -> names.contains(i.sagaFqn())
            && test.equals(i.sourceClassFqn()) && "update tournament successfully".equals(i.sourceMethodName())
            && i.sourceMode()==pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode.SAGAS).toList();
        if(inputs.size()!=2) throw new IllegalStateException("Expected two unchanged test inputs: "+inputs.size());
        var defs=model.sagaDefinitions().stream().filter(s -> names.contains(s.sagaFqn())).toList();
        List<Map<String,Object>> evidence=new ArrayList<>();
        Set<List<String>> fullOrders=new HashSet<>();
        for(var strategy:List.of(ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                                 ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED)) {
            String mode=strategy==ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED ? "compressed" : "full";
            var cfg=new ScenarioGeneratorConfig(true,ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,false,2,1000,10,1000,true,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,strategy,18092026L);
            // No selected event consequences: this experiment isolates normal Saga scheduling.
            var generated=ScenarioGenerator.generate(defs,inputs,List.of(),model.sourceSetupPlanBindings(),model.aggregateKeyInputEvidence(),cfg);
            int expected=mode.equals("full")?6:3;
            if(generated.workloadPlans().size()!=expected) throw new IllegalStateException(mode+" workloads: "+generated.counts()+generated.warnings());
            var materializable=new ArrayList<WorkloadMaterializability>();
            var computed=new ArrayList<ComputedVectorRecovery>();
            var scenarios=new LinkedHashMap<String,FaultScenario>();
            for(var w:generated.workloadPlans()) {
                var ready=EagerFaultScenarioGenerator.evaluateMaterializability(w);
                if(!ready.materializable() || w.setupPlan()==null || w.prerequisiteBaseline()!=null) throw new IllegalStateException(ready.toString());
                materializable.add(ready);
                Map<String,String> participants=w.participants().stream().collect(Collectors.toMap(SagaInstance::deterministicId,SagaInstance::sagaFqn));
                var order=w.forwardSchedule().stream().map(s -> participants.get(s.sagaInstanceId())+"::"+s.runtimeStepName()).toList();
                if(mode.equals("full")) fullOrders.add(order);
                else if(!fullOrders.contains(order)) throw new IllegalStateException("Compressed order absent from full enumeration");
                List<String> vectors=new ArrayList<>();
                vectors(w,0,new char[w.faultSlots().size()],vectors);
                for(String vector:vectors) {
                    var r=RecoveryScheduleGenerator.generate(w,vector,new RecoveryScheduleCap(10000));
                    if(!r.uncappedScheduleCount().equals(BigInteger.valueOf(r.writtenScheduleCount()))) throw new IllegalStateException("Truncated recovery");
                    computed.add(new ComputedVectorRecovery(w.deterministicId(),vector,FaultScenarioVectorSource.ON_DEMAND_REQUEST,r.uncappedScheduleCount(),r.writtenScheduleCount()));
                    for(var f:r.faultScenarios()) scenarios.put(f.deterministicId(),f);
                }
                evidence.add(Map.of("mode",mode,"workload",w.deterministicId(),"forwardOrder",order,"vectors",vectors.size(),
                    "setup",w.setupPlan(),"inputs",w.acceptedInputs().stream().map(InputVariant::deterministicId).toList()));
            }
            var result=new EagerFaultScenarioGenerationResult(generated,10000,List.copyOf(scenarios.values()),materializable,computed);
            new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter().write(model,application,result,output.resolve(mode));
            JSON.writeValue(output.resolve(mode+"-counts.json").toFile(),Map.of("workloads",generated.workloadPlans().size(),"vectors",computed.size(),"scenarios",scenarios.size(),"warnings",generated.warnings(),"computed",computed));
        }
        JSON.writeValue(output.resolve("selection.json").toFile(),evidence);
    }
    static void vectors(WorkloadPlan w,int participant,char[] bits,List<String> result) {
        if(participant==0) Arrays.fill(bits,'0');
        if(participant==w.participants().size()) { result.add(new String(bits));return; }
        vectors(w,participant+1,bits,result);
        String id=w.participants().get(participant).deterministicId();
        for(var slot:w.faultSlots()) if(id.equals(slot.sagaInstanceId())) {
            bits[slot.slotIndex()]='1';vectors(w,participant+1,bits,result);bits[slot.slotIndex()]='0';
        }
    }
    static void visitJava(ApplicationsFileTreeParser parser,Path root,String app,ApplicationAnalysisState state,
            java.util.function.BiConsumer<CompilationUnit,ApplicationAnalysisState> visitor) throws IOException {
        for(Path path:parser.getJavaFilePathsForApplication(root,app).values()) visitor.accept(StaticJavaParser.parse(path),state);
    }
}
