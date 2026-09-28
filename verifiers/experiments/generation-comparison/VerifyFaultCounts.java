import com.fasterxml.jackson.databind.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import java.nio.file.*;
import java.util.*;
import java.math.BigInteger;

/** Independent exhaustive comparison on small source-derived Quizzes sets, including 3/4 Sagas. */
public final class VerifyFaultCounts {
    static List<String> vectors(WorkloadPlan plan) {
        List<String> result = new ArrayList<>();
        vectors(plan, 0, new char[plan.faultSlots().size()], result); return result;
    }
    static void vectors(WorkloadPlan plan, int i, char[] vector, List<String> result) {
        if (i == plan.participants().size()) { result.add(new String(vector).replace('\0','0')); return; }
        vectors(plan,i+1,vector,result);
        for (var slot : plan.faultSlots()) if (slot.sagaInstanceId().equals(plan.participants().get(i).deterministicId())) {
            vector[slot.slotIndex()]='1'; vectors(plan,i+1,vector,result); vector[slot.slotIndex()]='0';
        }
    }
    public static void main(String[] args) throws Exception {
        var json = new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var model = json.readValue(Path.of(args[0]).toFile(),ScenarioModelAdapterResult.class);
        var normalized = InputVariantNormalizer.normalize(model.inputVariants(), new ScenarioGeneratorConfig(false,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false,4,100000,1,100000,true,ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,1L,100000,3));
        var byName = new TreeMap<String,SagaDefinition>();
        for (var s : model.sagaDefinitions()) if (normalized.inputsBySaga().containsKey(s.sagaFqn())) byName.put(s.sagaFqn(),s);
        List<Map<String,Object>> results = new ArrayList<>();
        // Select by source structure only. Include an emitter in each set; spread across
        // 2/3/4 Sagas and checkpoint/no-checkpoint shapes, bounded before seeing counts.
        for (int size=2;size<=4;size++) {
            int stepLimit = size + 2;
            var eligible = new ArrayList<List<String>>();
            combinations(List.copyOf(byName.keySet()),size,0,new ArrayList<>(),eligible);
            eligible.removeIf(set -> set.stream().mapToInt(s->byName.get(s).steps().size()).sum()>stepLimit
                    || model.eventConsequenceDefinitions().stream().noneMatch(e->set.contains(e.triggerSagaFqn())&&!set.contains(e.downstreamSagaFqn())));
            eligible.sort(Comparator.comparing(set->String.join("|",set)));
            // First, middle, last across the deterministic list; no outcome selection.
            for (int index : new TreeSet<>(List.of(0,eligible.size()/2,eligible.size()-1))) {
                var set=eligible.get(index); var sagas=set.stream().map(byName::get).toList();
                var inputs=set.stream().map(s->normalized.inputsBySaga().get(s).getFirst()).toList();
                for (var strategy : List.of(ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                        ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED)) {
                    var cfg=new ScenarioGeneratorConfig(false,ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                            ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,false,size,100000,1,100000,true,
                            ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,strategy,1L,100000,3);
                    var graph=ConflictGraphBuilder.buildSelectionGraph(sagas,model.eventConsequenceDefinitions(),cfg);
                    var anchors=InputTupleSelection.selectedAnchorCandidates(set,inputs,graph.conflictCandidates(),model.aggregateKeyInputEvidence(),InputTupleSelection.Mode.ALL);
                    var counted=new FaultScenarioStructureCounter(2000000).count(sagas,anchors,model.eventConsequenceDefinitions(),strategy,3);
                    var generated=ScenarioGenerator.generate(sagas,inputs,model.eventConsequenceDefinitions(),cfg);
                    if (generated.counts().getOrDefault("workloadsCapped",0)!=0) throw new IllegalStateException("Verification workload cap");
                    var plans=generated.workloadPlans().stream().filter(w->w.participants().size()==set.size()).toList();
                    BigInteger scenarios=BigInteger.ZERO;
                    long enumerated=0;
                    for (var plan:plans) for (String vector:vectors(plan)) {
                        var full=RecoveryScheduleGenerator.generate(plan,vector,100000);
                        if (!full.uncappedScheduleCount().equals(BigInteger.valueOf(full.writtenScheduleCount()))) throw new IllegalStateException("Verification recovery cap");
                        scenarios=scenarios.add(full.uncappedScheduleCount()); enumerated+=full.writtenScheduleCount();
                    }
                    if(!counted.workloads().equals(BigInteger.valueOf(plans.size()))||!counted.faultScenarios().equals(scenarios))
                        throw new IllegalStateException("Mismatch "+set+" "+strategy+" "+counted+" "+plans.size()+" "+scenarios);
                    results.add(Map.of("sagas",set,"schedule",strategy,"workloads",plans.size(),"faultScenarios",scenarios,"materializedScenarios",enumerated,"status","PASS"));
                    json.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(),results);
                }
            }
        }
        System.out.println("PASS "+results.size()+" comparisons; materialized scenarios="+results.stream().mapToLong(r->(Long)r.get("materializedScenarios")).sum());
    }
    static void combinations(List<String> names,int k,int at,List<String> prefix,List<List<String>> out) {
        if(prefix.size()==k){out.add(List.copyOf(prefix));return;}
        for(int i=at;i<=names.size()-(k-prefix.size());i++){prefix.add(names.get(i));combinations(names,k,i+1,prefix,out);prefix.removeLast();}
    }
}
