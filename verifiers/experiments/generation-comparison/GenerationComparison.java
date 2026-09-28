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
/** Static evaluation harness: production extraction, tuple selection and schedule enumeration. */
public final class GenerationComparison {
    static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    static final int VERIFY_MAX_ORDERS = 20_000;
    static final int VERIFY_MAX_STEP_OCCURRENCES = 200_000;
    static final Map<String, List<Case>> samples = new TreeMap<>();
    static final Map<String, Row> pairRows = new HashMap<>();
    static Path output;
    static Map<String, SagaDefinition> definitions;
    static ScenarioModelAdapterResult model;
    static long rowsWritten;
    static int verificationCap;

    public static void main(String[] args) throws Exception {
        Path applicationsRoot = Path.of(args[0]);
        String application = args[1];
        output = Path.of(args[2]);
        int[] inputCaps = Arrays.stream((args.length > 3 ? args[3] : "1,3").split(","))
            .mapToInt(Integer::parseInt).distinct().sorted().toArray();
        if (inputCaps.length == 0 || inputCaps[0] < 1) throw new IllegalArgumentException("Input caps must be positive");
        verificationCap = inputCaps[inputCaps.length - 1];
        
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

        model = new ApplicationAnalysisScenarioModelAdapter().adapt(state);

        // Same stable step order and IDs used by ScenarioGenerator's schedule inputs.
        definitions = new TreeMap<>();
        for (var saga : model.sagaDefinitions()) {
            var steps = saga.steps().stream().sorted(Comparator.comparingInt(StepDefinition::orderIndex)
                .thenComparing(StepDefinition::deterministicId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(StepDefinition::stepKey, Comparator.nullsFirst(String::compareTo))
                .thenComparing(StepDefinition::name, Comparator.nullsFirst(String::compareTo)))
                .map(s -> new StepDefinition(ScenarioIdGenerator.stepDefinitionId(saga.sagaFqn(), s),
                    s.stepKey(), s.name(), s.orderIndex(), s.predecessorStepKeys(), s.footprints(),
                    s.compensationFootprints(), s.compensationRegistered(), s.forwardAnalysisComplete(),
                    s.compensationAnalysisComplete(), s.compensationEvidence(), s.analysisDiagnostics(), s.warnings())).toList();
            definitions.put(saga.sagaFqn(), new SagaDefinition(saga.sagaFqn(), steps, saga.warnings()));
        }
        write("extraction.json", Map.of("counts", model.counts(), "diagnostics", model.diagnostics(),
            "sagas", definitions.values(), "scope", "forward schedules; no faults, recovery or event expansion"));
        if (args.length > 4 && args[4].equals("extract-only")) {
            write("model.json", model);
            return;
        }
        var strictGraph = ConflictGraphBuilder.build(List.copyOf(definitions.values()), config(3, false, false, ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 4));
        var broadGraph = ConflictGraphBuilder.build(List.copyOf(definitions.values()), config(3, true, false, ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 4));
        Map<String, List<ConflictGraphBuilder.ConflictCandidate>> strictPairs = indexPairs(strictGraph.conflictCandidates());
        Map<String, List<ConflictGraphBuilder.ConflictCandidate>> broadPairs = indexPairs(broadGraph.conflictCandidates());
        for (int inputCap : inputCaps) {
            var normalized = InputVariantNormalizer.normalize(model.inputVariants(), config(inputCap, false, false, ScenarioGeneratorConfig.ScheduleStrategy.SERIAL, 4));
            Map<String, List<InputVariant>> inputs = new TreeMap<>(normalized.inputsBySaga());
            inputs.keySet().retainAll(definitions.keySet());
            write("inputs-cap-" + inputCap + ".json", Map.<String,Object>of("normalization", normalized.counts(),
                "idsBySaga", inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                    e -> e.getValue().stream().map(InputVariant::deterministicId).toList(), (a,b) -> a, TreeMap::new))));
            List<String> names = List.copyOf(inputs.keySet());
            try (var writer = Files.newBufferedWriter(output.resolve("rows-cap-" + inputCap + ".jsonl"))) {
                for (int size = 2; size <= 4; size++) {
                    int selectedSize = size;
                    combinations(names, size, 0, new ArrayList<>(), set -> {
                        try {
                            var strict = candidates(set, strictPairs);
                            var broad = candidates(set, broadPairs);
                            BigInteger all = InputTupleSelection.count(set, inputs, List.of(), model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.ALL);
                            BigInteger s = InputTupleSelection.count(set, inputs, strict, model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.STRICT);
                            BigInteger b = InputTupleSelection.count(set, inputs, broad, model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
                            if (s.compareTo(b)>0 || b.compareTo(all)>0) throw new IllegalStateException("Selection is not nested");
                            var lengths = set.stream().map(n -> definitions.get(n).steps().size()).toList();
                            var sa = anchorCounts(set, strict);
                            var ba = anchorCounts(set, broad);
                            var row = new Row(set, lengths, all.toString(), s.toString(), b.toString(),
                                multinomial(lengths).toString(), multinomial(sa).toString(), multinomial(ba).toString(), sa, ba);
                            writer.write(JSON.writeValueAsString(row)); writer.newLine();
                            rowsWritten++;
                            if (sizeOf(set)==2 && inputCap==verificationCap) pairRows.put(String.join("|", set), row);
                            if (inputCap==verificationCap) {
                                if (s.signum()>0) consider(set, strict, "strict", row);
                                if (b.signum()>0) consider(set, broad, "type-fallback", row);
                            }
                            if (rowsWritten%1000==0) { writer.flush(); status("COUNTING", Map.of("inputCap",inputCap,"size",selectedSize,"rowsWritten",rowsWritten)); }
                        } catch (Exception e) { throw new RuntimeException(e); }
                    });
                    writer.flush();
                    status("COUNTING", Map.of("inputCap",inputCap,"completedSize",size,"rowsWritten",rowsWritten));
                }
            }
        }
        status("CROSS_CHECKING", Map.of("rowsWritten",rowsWritten));
        crossCheckAccounting();
        var chosen = samples.values().stream().flatMap(List::stream).sorted(Comparator.comparing(Case::key)).toList();
        write("verification-selection.json", chosen.stream().map(c -> Map.of("stratum",c.stratum(),"sagas",c.sagas(),"hash",c.key(),"lens",c.lens())).toList());
        try (var writer = Files.newBufferedWriter(output.resolve("verification.jsonl"))) {
            for (var c : chosen) {
                var result = verify(c);
                writer.write(JSON.writeValueAsString(result));writer.newLine();writer.flush();
                status("VERIFYING", Map.of("case",c.stratum(),"sagas",c.sagas()));
            }
        }
        status("COMPLETE", Map.of("rowsWritten",rowsWritten,"verificationCases",chosen.size()));
    }

    static int sizeOf(List<String> set) { return set.size(); }
    static ScenarioGeneratorConfig config(int cap, boolean broad, boolean brute, ScenarioGeneratorConfig.ScheduleStrategy strategy, int size) {
        return new ScenarioGeneratorConfig(true, brute ? ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE : ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
            ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, false, size, 1, cap, 5000, broad,
            ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE, strategy, 1234L, 100000, 1);
    }
    static void visitJava(ApplicationsFileTreeParser parser, Path root, String app, ApplicationAnalysisState state,
            java.util.function.BiConsumer<CompilationUnit, ApplicationAnalysisState> visitor) throws IOException {
        for (Path path : parser.getJavaFilePathsForApplication(root, app).values()) visitor.accept(StaticJavaParser.parse(path), state);
    }
    static void combinations(List<String> names, int size, int from, List<String> current, java.util.function.Consumer<List<String>> consume) {
        if (current.size()==size) { consume.accept(List.copyOf(current)); return; }
        for (int i=from; i<=names.size()-(size-current.size()); i++) {
            current.add(names.get(i)); combinations(names,size,i+1,current,consume); current.remove(current.size()-1);
        }
    }
    static Map<String,List<ConflictGraphBuilder.ConflictCandidate>> indexPairs(List<ConflictGraphBuilder.ConflictCandidate> cs) {
        Map<String,List<ConflictGraphBuilder.ConflictCandidate>> result=new HashMap<>();
        for(var c:cs) result.computeIfAbsent(c.leftSagaFqn()+"|"+c.rightSagaFqn(), k->new ArrayList<>()).add(c);
        return result;
    }
    static List<ConflictGraphBuilder.ConflictCandidate> candidates(List<String> set, Map<String,List<ConflictGraphBuilder.ConflictCandidate>> pairs) {
        var result=new ArrayList<ConflictGraphBuilder.ConflictCandidate>();
        for(int i=0;i<set.size();i++) for(int j=i+1;j<set.size();j++) result.addAll(pairs.getOrDefault(set.get(i)+"|"+set.get(j), List.of()));
        return result;
    }
    static Set<String> anchors(List<ConflictGraphBuilder.ConflictCandidate> cs) {
        Set<String> ids=new HashSet<>();
        for(var c:cs) { ids.add(c.leftStepId()); ids.add(c.rightStepId()); }
        return ids;
    }
    static List<Integer> anchorCounts(List<String> set,List<ConflictGraphBuilder.ConflictCandidate> cs) {
        var ids=anchors(cs);
        return set.stream().map(n->(int)definitions.get(n).steps().stream().filter(s->ids.contains(s.deterministicId())).count()).toList();
    }
    static BigInteger factorial(int n) { BigInteger x=BigInteger.ONE;for(int i=2;i<=n;i++)x=x.multiply(BigInteger.valueOf(i));return x; }
    static BigInteger multinomial(List<Integer> counts) {
        BigInteger n=factorial(counts.stream().mapToInt(Integer::intValue).sum());
        for(int c:counts)n=n.divide(factorial(c));
        return n;
    }
    static void consider(List<String> set,List<ConflictGraphBuilder.ConflictCandidate> cs,String lens,Row row) throws Exception {
        BigInteger full=new BigInteger(row.fullOrders());
        int steps=row.stepCounts().stream().mapToInt(Integer::intValue).sum();
        if(full.compareTo(BigInteger.valueOf(VERIFY_MAX_ORDERS))>0 || full.multiply(BigInteger.valueOf(steps)).compareTo(BigInteger.valueOf(VERIFY_MAX_STEP_OCCURRENCES))>0)return;
        int bin=full.compareTo(BigInteger.valueOf(100))<=0?100:full.compareTo(BigInteger.valueOf(1000))<=0?1000:VERIFY_MAX_ORDERS;
        String stratum=set.size()+"/"+lens+"/"+bin;
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("|",set).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var list=samples.computeIfAbsent(stratum,k->new ArrayList<>());
        list.add(new Case(stratum,hash,lens,set,List.copyOf(cs)));
        list.sort(Comparator.comparing(Case::key));
        if(list.size()>2)list.removeLast();
    }
    static Map<String,Object> verify(Case c) {
        var inputs=c.sagas().stream().map(n->new ScheduleEnumerator.SagaScheduleInput(n,n,definitions.get(n).steps())).toList();
        int fullExpected=multinomial(c.sagas().stream().map(n->definitions.get(n).steps().size()).toList()).intValueExact();
        int smallExpected=multinomial(anchorCounts(c.sagas(),c.candidates())).intValueExact();
        var full=ScheduleEnumerator.enumerate(inputs,ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,fullExpected+1,1234L,c.candidates());
        var small=ScheduleEnumerator.enumerate(inputs,ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,fullExpected+1,1234L,c.candidates());
        if(full.schedules().size()!=fullExpected || small.schedules().size()!=smallExpected || !full.warnings().isEmpty() || !small.warnings().isEmpty())throw new IllegalStateException("Unexpected count/cap "+c);
        var ids=anchors(c.candidates());
        var a=projections(full.schedules(),ids,inputs);
        var b=projections(small.schedules(),ids,inputs);
        if(!a.equals(b) || a.size()!=smallExpected)throw new IllegalStateException("Anchor order mismatch "+c);
        var result=new LinkedHashMap<String,Object>();
        result.put("stratum",c.stratum());result.put("sagas",c.sagas());result.put("lens",c.lens());
        result.put("fullOrders",fullExpected);result.put("compressedOrders",smallExpected);result.put("anchorOrders",a.size());result.put("passed",true);
        return result;
    }
    static Set<List<String>> projections(List<List<ScheduledStep>> schedules,Set<String> anchors,List<ScheduleEnumerator.SagaScheduleInput> inputs) {
        Set<List<String>> projected=new HashSet<>();Set<List<String>> unique=new HashSet<>();
        for(var schedule:schedules) {
            List<String> sequence=schedule.stream().map(ScheduledStep::stepId).toList();
            if(!unique.add(sequence))throw new IllegalStateException("Duplicate schedule");
            for(var input:inputs) {
                var actual=schedule.stream().filter(s->s.sagaInstanceId().equals(input.sagaInstanceId())).map(ScheduledStep::stepId).toList();
                if(!actual.equals(input.steps().stream().map(StepDefinition::deterministicId).toList()))throw new IllegalStateException("In-Saga order changed");
            }
            projected.add(sequence.stream().filter(anchors::contains).toList());
        }
        return projected;
    }
    static void crossCheckAccounting() throws Exception {
        int compared=0;
        for(String mode:List.of("brute","strict","broad"))for(var strategy:List.of(ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED)) {
            boolean broad=mode.equals("broad");boolean brute=mode.equals("brute");
            var report=new ScenarioSpaceAccountingCalculator().calculate("quizzes",List.copyOf(definitions.values()),model.inputVariants(),model.sourceSetupPlanBindings(),model.aggregateKeyInputEvidence(),config(verificationCap,broad,brute,strategy,2),0);
            if(report.groupedSagaSets().size()!=pairRows.size())throw new IllegalStateException("Pair row coverage mismatch");
            for(var r:report.groupedSagaSets()) {
                var row=pairRows.get(r.sagaSetKey());
                String tuples=brute?row.allTuples():broad?row.broadTuples():row.strictTuples();
                String orders=strategy==ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING?row.fullOrders():broad?row.broadCompressed():row.strictCompressed();
                String capped=new BigInteger(orders).min(BigInteger.valueOf(5000)).toString();
                if(!tuples.equals(r.compatibleInputTupleCount())||!capped.equals(r.scheduleCountPerTuple()))throw new IllegalStateException("Accounting mismatch "+r.sagaSetKey());
                compared++;
            }
        }
        write("accounting-cross-check.json",Map.of("passed",true,"comparisons",compared,"scheduleCap",5000,"inputCap",verificationCap));
    }
    static void write(String file,Object value)throws Exception { JSON.writerWithDefaultPrettyPrinter().writeValue(output.resolve(file).toFile(),value); }
    static void status(String stage,Map<String,?> details)throws Exception { write("status.json",Map.of("stage",stage,"details",details,"at",java.time.Instant.now().toString()));System.out.println(stage+" "+details); }
    record Row(List<String> sagas,List<Integer> stepCounts,String allTuples,String strictTuples,String broadTuples,String fullOrders,String strictCompressed,String broadCompressed,List<Integer> strictAnchors,List<Integer> broadAnchors){}
    record Case(String stratum,String key,String lens,List<String> sagas,List<ConflictGraphBuilder.ConflictCandidate> candidates){}
}
