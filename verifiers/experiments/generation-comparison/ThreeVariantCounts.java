import java.nio.file.*;
import java.util.*;
import java.math.BigInteger;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.*;

/** Current broad-selection counts. Fails closed if inputs have exact logical bindings. */
public final class ThreeVariantCounts {
    public static void main(String[] args) throws Exception {
        GenerationComparison.main(new String[]{args[0], args[1], args[2], "10000", "extract-only"});
        var model = GenerationComparison.model;
        var defs = GenerationComparison.definitions;
        var config = GenerationComparison.config(10000, true, false, ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED, 4);
        var normalized = InputVariantNormalizer.normalize(model.inputVariants(), config);
        var inputs = new TreeMap<>(normalized.inputsBySaga());
        inputs.keySet().retainAll(defs.keySet());
        if (normalized.inputs().stream().anyMatch(i -> !i.logicalKeyBindings().isEmpty()))
            throw new IllegalStateException("Input-independent counting requires no exact input bindings");
        if (normalized.counts().entrySet().stream().anyMatch(e -> e.getKey().toLowerCase().contains("cap") && e.getValue() > 0))
            throw new IllegalStateException("Input cap excluded inputs");
        GenerationComparison.write("inputs.json", normalized);
        var graph = ConflictGraphBuilder.buildSelectionGraph(List.copyOf(defs.values()), model.eventConsequenceDefinitions(), config);
        var pairs = GenerationComparison.indexPairs(graph.conflictCandidates());
        var totals = new TreeMap<Integer,Map<String,BigInteger>>();
        var names = List.copyOf(inputs.keySet());
        long[] checkedTuples = {0};
        int[] setCount = {0};
        try (var writer = Files.newBufferedWriter(Path.of(args[2]).resolve("counts.jsonl"))) {
            for (int size=2; size<=4; size++) {
                var sums = new TreeMap<String,BigInteger>();
                for (var key : List.of("sagaSets", "selectedSagaSets", "inputCombinations", "selectedInputCombinations", "allOrders", "prunedOrders", "compressedOrders")) sums.put(key, BigInteger.ZERO);
                totals.put(size, sums);
                GenerationComparison.combinations(names,size,0,new ArrayList<>(), set -> {
                    try {
                        var candidates = GenerationComparison.candidates(set,pairs);
                        var representative = set.stream().map(s -> inputs.get(s).getFirst()).toList();
                        var all = InputTupleSelection.count(set,inputs,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.ALL);
                        var kept = InputTupleSelection.count(set,inputs,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
                        boolean selected = InputTupleSelection.selected(set,representative,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
                        if (!kept.equals(selected ? all : BigInteger.ZERO)) throw new IllegalStateException("Count/selection disagreement");
                        var anchors = InputTupleSelection.selectedAnchorCandidates(set,representative,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
                        // Check every alternative input in every pair against the representative anchors.
                        // Exact bindings are absent: broad matching can differ only by UNEQUAL keys,
                        // which source-identity evidence cannot establish.
                        if (set.size()==2) for (int side=0;side<2;side++) for (var input: inputs.get(set.get(side))) {
                            var tuple = new ArrayList<>(representative); tuple.set(side,input);
                            if (!anchors.equals(InputTupleSelection.selectedAnchorCandidates(set,tuple,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK))) throw new IllegalStateException("Input-dependent anchors");
                            checkedTuples[0]++;
                        }
                        var lengths = set.stream().map(s -> defs.get(s).steps().size()).toList();
                        var full = GenerationComparison.multinomial(lengths);
                        var compressed = GenerationComparison.multinomial(GenerationComparison.anchorCounts(set,anchors));
                        var row = new LinkedHashMap<String,Object>();
                        row.put("sagas",set); row.put("inputCombinations",all.toString()); row.put("selectedInputCombinations",kept.toString()); row.put("ordersPerCombination",full.toString()); row.put("compressedOrdersPerCombination",compressed.toString());
                        writer.write(GenerationComparison.JSON.writeValueAsString(row));writer.newLine();
                        sums.merge("sagaSets",BigInteger.ONE,BigInteger::add);
                        sums.merge("selectedSagaSets",selected?BigInteger.ONE:BigInteger.ZERO,BigInteger::add);
                        sums.merge("inputCombinations",all,BigInteger::add);
                        sums.merge("selectedInputCombinations",kept,BigInteger::add);
                        sums.merge("allOrders",all.multiply(full),BigInteger::add);
                        sums.merge("prunedOrders",kept.multiply(full),BigInteger::add);
                        sums.merge("compressedOrders",kept.multiply(compressed),BigInteger::add);
                        if (selected) GenerationComparison.consider(set,anchors,"type-fallback",new GenerationComparison.Row(set,lengths,all.toString(),"0",kept.toString(),full.toString(),"0",compressed.toString(),List.of(),GenerationComparison.anchorCounts(set,anchors)));
                        setCount[0]++;
                    } catch(Exception e) {throw new RuntimeException(e);}
                });
                writer.flush();
                GenerationComparison.status("COUNTING",Map.of("size",size,"totals",sums));
            }
        }
        // Independently check production grouped accounting with one input per Saga.
        var accounting = new ScenarioSpaceAccountingCalculator().calculate(args[1],List.copyOf(defs.values()),model.inputVariants(),model.eventConsequenceDefinitions(),model.sourceSetupPlanBindings(),model.aggregateKeyInputEvidence(),GenerationComparison.config(1,true,false,ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED,2),0);
        var one = InputVariantNormalizer.normalize(model.inputVariants(),GenerationComparison.config(1,true,false,ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,2)).inputsBySaga();
        int accountingChecks=0;
        for(var row:accounting.groupedSagaSets()) {
            var set=row.sagaFqns();
            var candidates=GenerationComparison.candidates(set,pairs);
            var tuple=set.stream().map(s->one.get(s).getFirst()).toList();
            boolean selected=InputTupleSelection.selected(set,tuple,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
            var anchors=InputTupleSelection.selectedAnchorCandidates(set,tuple,candidates,model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK);
            var expected=selected?GenerationComparison.multinomial(GenerationComparison.anchorCounts(set,anchors)).min(BigInteger.valueOf(5000)):BigInteger.ZERO;
            if(!expected.toString().equals(row.scenarioShapeCount())) throw new IllegalStateException("Production accounting mismatch "+row.sagaSetKey()+" "+expected+" "+row.scenarioShapeCount());
            accountingChecks++;
        }
        GenerationComparison.write("accounting-cap1.json",accounting);
        var verification=new ArrayList<Map<String,Object>>();
        for(var sample:GenerationComparison.samples.values()) for(var c:sample) verification.add(GenerationComparison.verify(c));
        GenerationComparison.write("enumeration-verification.json",verification);
        GenerationComparison.write("summary.json",Map.of("application",args[1],"extraction",model.counts(),"normalization",normalized.counts(),"sagasWithInputs",inputs.size(),"totals",totals,"proof",Map.of("accountingPairChecks",accountingChecks,"alternativeInputChecks",checkedTuples[0],"fullEnumerationChecks",verification.size(),"noExactInputBindings",true),"scope","Normal step orders, 2–4 distinct Saga types, all accepted test inputs; current broad interaction pruning includes compensation/event effects. Fault vectors, recovery schedules and event placements are not expanded; executability is not established."));
        GenerationComparison.status("COMPLETE",Map.of("sagaSets",setCount[0],"enumerationChecks",verification.size()));
    }
}
