package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;

/** Count-only entry point using production input admission, pruning and event rules.
 * Schedule/catalogue/recovery write caps do not limit these structural counts.
 * Input and event-selection limits remain part of the configured domain.
 */
public final class FaultScenarioCountService {
    public record Row(List<String> sagas, String status, BigInteger inputCombinations,
                      BigInteger workloads, BigInteger faultScenarios, String diagnostic) {}
    public record Totals(int sagaTypes, String status, int countedSets, int incompleteSets,
                         BigInteger workloads, BigInteger faultScenarios) {}
    public record Report(String scope, ScenarioGeneratorConfig config, Map<String, Integer> inputNormalization,
                         List<Totals> totals, int cachedStructures) {}
    private final FaultScenarioStructureCounter counter;
    private final long maxExplicitInputTuples;
    public FaultScenarioCountService(long maxStates, long maxExplicitInputTuples) {
        counter = new FaultScenarioStructureCounter(maxStates);
        if (maxExplicitInputTuples < 1) throw new IllegalArgumentException("maxExplicitInputTuples must be positive");
        this.maxExplicitInputTuples = maxExplicitInputTuples;
    }
    public Report count(ScenarioModelAdapterResult model, ScenarioGeneratorConfig config, Consumer<Row> sink) {
        var normalized = InputVariantNormalizer.normalize(model.inputVariants(), config);
        var definitions = new TreeMap<String, SagaDefinition>();
        for (var saga : model.sagaDefinitions()) {
            if (definitions.putIfAbsent(saga.sagaFqn(), saga) != null) throw new IllegalArgumentException("Duplicate Saga definition: " + saga.sagaFqn());
        }
        var inputs = new TreeMap<>(normalized.inputsBySaga());
        inputs.keySet().retainAll(definitions.keySet());
        var graph = ConflictGraphBuilder.buildSelectionGraph(List.copyOf(definitions.values()), model.eventConsequenceDefinitions(), config);
        var mode = config.generationStrategy() == ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE ? InputTupleSelection.Mode.ALL
                : config.allowTypeOnlyFallback() ? InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK : InputTupleSelection.Mode.STRICT;
        List<Totals> totals = new ArrayList<>();
        List<String> names = List.copyOf(inputs.keySet());
        for (int k = config.includeSingles() ? 1 : 2; k <= Math.min(config.maxSagaSetSize(), names.size()); k++) {
            int[] complete = {0}, incomplete = {0};
            BigInteger[] sums = {BigInteger.ZERO, BigInteger.ZERO};
            combinations(names, k, 0, new ArrayList<>(), set -> {
                var candidates = graph.conflictCandidates().stream().filter(c -> set.contains(c.leftSagaFqn()) && set.contains(c.rightSagaFqn())).toList();
                var effectiveMode = set.size() == 1 ? InputTupleSelection.Mode.ALL : mode;
                var tupleCount = InputTupleSelection.count(set, inputs, candidates, model.aggregateKeyInputEvidence(), effectiveMode);
                try {
                    FaultScenarioStructureCounter.Counts result;
                    boolean homogeneous = effectiveMode == InputTupleSelection.Mode.ALL || effectiveMode == InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK
                            && set.stream().flatMap(s -> inputs.get(s).stream()).allMatch(i -> i.logicalKeyBindings().isEmpty());
                    if (tupleCount.signum() == 0) result = FaultScenarioStructureCounter.Counts.zero();
                    else if (homogeneous) {
                        var tuple = set.stream().map(s -> inputs.get(s).getFirst()).toList();
                        var anchors = InputTupleSelection.selectedAnchorCandidates(set, tuple, candidates, model.aggregateKeyInputEvidence(), effectiveMode);
                        result = counter.count(set.stream().map(definitions::get).toList(), anchors, model.eventConsequenceDefinitions(),
                                set.size() == 1 ? ScenarioGeneratorConfig.ScheduleStrategy.SERIAL : config.scheduleStrategy(), config.maxEventConsequencesPerWorkload()).multiply(tupleCount);
                    } else {
                        BigInteger all = InputTupleSelection.count(set, inputs, candidates, model.aggregateKeyInputEvidence(), InputTupleSelection.Mode.ALL);
                        if (all.compareTo(BigInteger.valueOf(maxExplicitInputTuples)) > 0) throw new IllegalStateException("Input-dependent tuple count exceeds explicit tuple limit " + maxExplicitInputTuples);
                        var sum = new FaultScenarioStructureCounter.Counts[]{FaultScenarioStructureCounter.Counts.zero()};
                        tuples(set, inputs, 0, new ArrayList<>(), tuple -> {
                            if (InputTupleSelection.selected(set, tuple, candidates, model.aggregateKeyInputEvidence(), effectiveMode)) {
                                var anchors = InputTupleSelection.selectedAnchorCandidates(set, tuple, candidates, model.aggregateKeyInputEvidence(), effectiveMode);
                                sum[0] = sum[0].add(counter.count(set.stream().map(definitions::get).toList(), anchors, model.eventConsequenceDefinitions(),
                                        set.size() == 1 ? ScenarioGeneratorConfig.ScheduleStrategy.SERIAL : config.scheduleStrategy(), config.maxEventConsequencesPerWorkload()));
                            }
                        });
                        result = sum[0];
                    }
                    complete[0]++;
                    sums[0] = sums[0].add(result.workloads()); sums[1] = sums[1].add(result.faultScenarios());
                    sink.accept(new Row(set, "COMPLETE", tupleCount, result.workloads(), result.faultScenarios(), null));
                } catch (IllegalStateException | IllegalArgumentException e) {
                    incomplete[0]++;
                    sink.accept(new Row(set, "INCOMPLETE", tupleCount, null, null, e.getMessage()));
                }
            });
            // Never expose the subtotal as an exact total when any set is missing.
            totals.add(new Totals(k, incomplete[0] == 0 ? "COMPLETE" : "INCOMPLETE", complete[0], incomplete[0],
                    incomplete[0] == 0 ? sums[0] : null, incomplete[0] == 0 ? sums[1] : null));
        }
        return new Report("Structural candidates; canonical no-fault/first-fault choices; supported event selections and recovery; not runtime executability", config, normalized.counts(), List.copyOf(totals), counter.cachedStructures());
    }
    private static void combinations(List<String> names, int k, int at, List<String> prefix, Consumer<List<String>> sink) {
        if (prefix.size() == k) { sink.accept(List.copyOf(prefix)); return; }
        for (int i = at; i <= names.size() - (k - prefix.size()); i++) {
            prefix.add(names.get(i)); combinations(names, k, i + 1, prefix, sink); prefix.removeLast();
        }
    }
    private static void tuples(List<String> set, Map<String,List<InputVariant>> inputs, int at, List<InputVariant> prefix, Consumer<List<InputVariant>> sink) {
        if (at == set.size()) { sink.accept(List.copyOf(prefix)); return; }
        for (var input : inputs.get(set.get(at))) {
            prefix.add(input); tuples(set, inputs, at + 1, prefix, sink); prefix.removeLast();
        }
    }
}
