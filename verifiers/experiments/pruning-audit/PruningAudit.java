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
public final class PruningAudit {
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



        var cfg=config(ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED);
        var normalized=InputVariantNormalizer.normalize(model.inputVariants(),cfg);
        var bySaga=new TreeMap<String,List<InputVariant>>(normalized.inputsBySaga());
        var defs=new TreeMap<String,SagaDefinition>();
        model.sagaDefinitions().forEach(s -> defs.put(s.sagaFqn(),s));
        bySaga.keySet().retainAll(defs.keySet());
        if (args.length > 3 && args[3].equals("selection-counts")) {
            selectionCounts(model, bySaga, cfg, output);
            return;
        }
        var graph=ConflictGraphBuilder.build(model.sagaDefinitions(),cfg);
        List<Map<String,Object>> rows=new ArrayList<>();
        var names=List.copyOf(bySaga.keySet());
        for(int a=0;a<names.size();a++) for(int b=a+1;b<names.size();b++) {
            var pair=List.of(names.get(a),names.get(b));
            var edges=graph.conflictCandidates().stream().filter(c -> pair.contains(c.leftSagaFqn()) && pair.contains(c.rightSagaFqn())).toList();
            var all=InputTupleSelection.count(pair,bySaga,edges,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.ALL);
            var kept=InputTupleSelection.count(pair,bySaga,edges,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
            List<EventConsequenceDefinition> eventBridges=new ArrayList<>();
            for(var e:model.eventConsequenceDefinitions()) {
                if(!pair.contains(e.triggerSagaFqn()) || !defs.containsKey(e.downstreamSagaFqn())) continue;
                String other=pair.get(0).equals(e.triggerSagaFqn())?pair.get(1):pair.get(0);
                var reached=accesses(defs.get(e.downstreamSagaFqn()),false);
                var target=accesses(defs.get(other),false);
                if(interacts(reached,target)) eventBridges.add(e);
            }
            var ar=accesses(defs.get(pair.get(0)),true); var br=accesses(defs.get(pair.get(1)),true);
            var af=accesses(defs.get(pair.get(0)),false); var bf=accesses(defs.get(pair.get(1)),false);
            var row=new LinkedHashMap<String,Object>();
            row.put("sagas",pair); row.put("allTuples",all);row.put("selectedTuples",kept);
            row.put("directConflictEdges",edges.size());row.put("forwardAccesses",List.of(af,bf));
            row.put("compensationAccesses",List.of(ar,br));
            row.put("potentialRecoveryInteraction",interacts(ar,bf)||interacts(br,af)||interacts(ar,br));
            row.put("potentialEventBridges",eventBridges);
            row.put("sameSourceTuples",bySaga.get(pair.get(0)).stream().mapToLong(i -> bySaga.get(pair.get(1)).stream().filter(j -> sameSource(i,j)).count()).sum());
            rows.add(row);
        }
        JSON.writeValue(output.resolve("inventory.json").toFile(),Map.of("normalization",normalized.counts(),"pairs",rows,"events",model.eventConsequenceDefinitions()));
        JSON.writeValue(output.resolve("inputs.json").toFile(),bySaga);
        List<Map<String,Object>> probes=new ArrayList<>();
        // Fixed structural probes, selected before application outcomes.
        for(var pairNames:List.of(List.of("UpdateStudentName","FindTournament"),
                List.of("GetCourseExecutionById","FindTournament"),
                List.of("UpdateUserName","DeactivateUser"),
                List.of("UpdateTournament","FindTournament"))) {
            var pair=names.stream().filter(n -> pairNames.contains(shortName(n))).sorted().toList();
            if(pair.size()!=2) throw new IllegalStateException("Missing probe "+pairNames);
            List<List<InputVariant>> tuples=new ArrayList<>();
            for(var i:bySaga.get(pair.get(0))) for(var j:bySaga.get(pair.get(1))) if(sameSource(i,j)) tuples.add(List.of(i,j));
            tuples.sort(Comparator.comparing(t -> t.get(0).deterministicId()+t.get(1).deterministicId()));
            var probe=new LinkedHashMap<String,Object>();probe.put("sagas",pair);probe.put("sameSourceTuples",tuples.size());
            if(tuples.isEmpty()) {probe.put("status","NO_SAME_SOURCE_TUPLE");probes.add(probe);continue;}
            var tuple=tuples.get(0);probe.put("inputs",tuple);
            String label=String.join("-",pairNames);
            for(var strategy:ScenarioGeneratorConfig.GenerationStrategy.values()) {
                // Keep every extracted definition available so event-mediated selection can
                // inspect the downstream Saga even though only the probed pair has inputs.
                var generated=ScenarioGenerator.generate(model.sagaDefinitions(),tuple,model.eventConsequenceDefinitions(),model.sourceSetupPlanBindings(),model.aggregateKeyInputEvidence(),config(strategy));
                var details=new LinkedHashMap<String,Object>();
                details.put("counts",generated.counts());details.put("warnings",generated.warnings());
                details.put("workloads",generated.workloadPlans().stream().map(w -> {
                    var x=new LinkedHashMap<String,Object>();x.put("id",w.deterministicId());x.put("events",w.eventConsequences());
                    x.put("materializability",EagerFaultScenarioGenerator.evaluateMaterializability(w));x.put("setupPresent",w.setupPlan()!=null);
                    return x;
                }).toList());
                probe.put(strategy.name(),details);
                // Persist ordinary packages, without expanding faults or running application code.
                var result=new EagerFaultScenarioGenerationResult(generated,10000,List.of(),generated.workloadPlans().stream().map(EagerFaultScenarioGenerator::evaluateMaterializability).toList(),List.of());
                new ExecutableArtifactWriter().write(model,application,result,output.resolve(label).resolve(strategy.name()));
            }
            probes.add(probe);
        }
        JSON.writeValue(output.resolve("probes.json").toFile(),probes);
    }
    static String shortName(String n){return n.substring(n.lastIndexOf('.')+1).replace("FunctionalitySagas","");}
    static void selectionCounts(ScenarioModelAdapterResult model, Map<String,List<InputVariant>> inputs,
                                ScenarioGeneratorConfig cfg, Path output) throws Exception {
        var forward=ConflictGraphBuilder.build(model.sagaDefinitions(),cfg).conflictCandidates();
        var expanded=ConflictGraphBuilder.buildSelectionGraph(model.sagaDefinitions(),model.eventConsequenceDefinitions(),cfg).conflictCandidates();
        var pairs=new TreeMap<String,List<ConflictGraphBuilder.ConflictCandidate>>();
        for(var edge:expanded) pairs.computeIfAbsent(pairKey(edge.leftSagaFqn(),edge.rightSagaFqn()),k->new ArrayList<>()).add(edge);
        var names=List.copyOf(inputs.keySet());
        Map<String,Object> summary=new LinkedHashMap<>();
        summary.put("scope","Input combinations of distinct Saga types; no scheduling, event placements, faults or executable-case claim");
        summary.put("inputs",inputs.values().stream().mapToInt(List::size).sum());
        summary.put("sagaTypes",names.size());
        summary.put("inputIds",inputs.values().stream().flatMap(List::stream).map(InputVariant::deterministicId).sorted().toList());
        Map<Integer,Object> totals=new TreeMap<>();
        for(int size=2;size<=4;size++) {
            BigInteger[] sum={BigInteger.ZERO,BigInteger.ZERO,BigInteger.ZERO}; long[] sets={0,0,0};
            try(var writer=Files.newBufferedWriter(output.resolve("selection-size-"+size+".jsonl"))) {
                combinations(names,size,0,new ArrayList<>(),set->{
                    var edges=new ArrayList<ConflictGraphBuilder.ConflictCandidate>();
                    for(int a=0;a<set.size();a++)for(int b=a+1;b<set.size();b++) edges.addAll(pairs.getOrDefault(pairKey(set.get(a),set.get(b)),List.of()));
                    // Direct graph independently supplies the reference; indirect evidence is additive.
                    var direct=forward.stream().filter(e->set.contains(e.leftSagaFqn())&&set.contains(e.rightSagaFqn())).toList();
                    BigInteger all=InputTupleSelection.count(set,inputs,List.of(),model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.ALL);
                    BigInteger before=InputTupleSelection.count(set,inputs,direct,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
                    BigInteger after=InputTupleSelection.count(set,inputs,edges,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
                    if(before.compareTo(after)>0||after.compareTo(all)>0)throw new IllegalStateException("Non-additive selection");
                    sum[0]=sum[0].add(all);sum[1]=sum[1].add(before);sum[2]=sum[2].add(after);
                    sets[0]++;if(before.signum()>0)sets[1]++;if(after.signum()>0)sets[2]++;
                    try {writer.write(JSON.writeValueAsString(Map.of("sagas",set,"all",all,"forwardOnly",before,"current",after)));writer.newLine();}
                    catch(IOException e){throw new RuntimeException(e);}
                });
            }
            totals.put(size,Map.of("allTuples",sum[0],"forwardOnlyTuples",sum[1],"currentTuples",sum[2],
                    "allSets",sets[0],"forwardOnlySets",sets[1],"currentSets",sets[2]));
            summary.put("totals",totals);JSON.writeValue(output.resolve("selection-counts.json").toFile(),summary);
        }
    }
    static String pairKey(String a,String b){return a.compareTo(b)<0?a+"|"+b:b+"|"+a;}
    static void combinations(List<String> names,int size,int from,List<String> current,java.util.function.Consumer<List<String>> consume){
        if(current.size()==size){consume.accept(List.copyOf(current));return;}
        for(int i=from;i<=names.size()-(size-current.size());i++){current.add(names.get(i));combinations(names,size,i+1,current,consume);current.removeLast();}
    }
    static boolean sameSource(InputVariant a,InputVariant b){return Objects.equals(a.sourceClassFqn(),b.sourceClassFqn()) && Objects.equals(a.sourceMethodName(),b.sourceMethodName());}
    static Map<String,Set<String>> accesses(SagaDefinition s,boolean recovery){
        var map=new TreeMap<String,Set<String>>();
        for(var step:s.steps()) for(var f:recovery?step.compensationFootprints():step.footprints()) {
            if(f.aggregateKey()==null)continue;
            var key=f.aggregateKey().aggregateName();if(key==null)key=f.aggregateKey().aggregateTypeName();
            if(key!=null)map.computeIfAbsent(key,k -> new TreeSet<>()).add(f.accessMode().name());
        }
        return map;
    }
    static boolean interacts(Map<String,Set<String>> a,Map<String,Set<String>> b){
        return a.entrySet().stream().anyMatch(e -> b.containsKey(e.getKey()) && (e.getValue().contains("WRITE")||b.get(e.getKey()).contains("WRITE")));
    }
    static ScenarioGeneratorConfig config(ScenarioGeneratorConfig.GenerationStrategy strategy){
        return new ScenarioGeneratorConfig(true,strategy,ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,false,2,10000,1000,10000,true,
            ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,19092026L,100000,3);
    }
    static void visitJava(ApplicationsFileTreeParser parser,Path root,String app,ApplicationAnalysisState state,
            java.util.function.BiConsumer<CompilationUnit,ApplicationAnalysisState> visitor) throws IOException {
        for(Path path:parser.getJavaFilePathsForApplication(root,app).values()) visitor.accept(StaticJavaParser.parse(path),state);
    }
}
