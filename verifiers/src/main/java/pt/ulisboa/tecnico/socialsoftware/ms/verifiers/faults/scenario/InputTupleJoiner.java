package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceAggregateKeyInputEvidence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class InputTupleJoiner {

    private InputTupleJoiner() {
    }

    public static Result join(List<String> sagaOrder, Map<String, List<InputVariant>> inputsBySaga) {
        return join(sagaOrder, inputsBySaga, List.of(), List.of(), InputTupleSelection.Mode.ALL);
    }

    public static Result join(List<String> sagaOrder,
                              Map<String, List<InputVariant>> inputsBySaga,
                              List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates,
                              List<SourceAggregateKeyInputEvidence> sourceEvidence,
                              InputTupleSelection.Mode mode) {
        List<String> orderedSagas = sagaOrder == null ? List.of() : sagaOrder.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        LinkedHashMap<String, InputTuple> tuples = new LinkedHashMap<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

        if (orderedSagas.isEmpty()) {
            counts.put("inputTuplesSeen", 0);
            counts.put("inputTuplesEmitted", 0);
            counts.put("inputTuplesDeduplicated", 0);
            return new Result(List.of(), Collections.unmodifiableMap(counts), List.copyOf(warnings));
        }

        List<List<InputVariant>> inputCandidates = new ArrayList<>();
        for (String sagaFqn : orderedSagas) {
            List<InputVariant> sagaInputs = inputsBySaga == null ? null : inputsBySaga.get(sagaFqn);
            if (sagaInputs == null || sagaInputs.isEmpty()) {
                warnings.add("no usable inputs for saga " + sagaFqn);
                counts.put("inputTuplesSeen", 0);
                counts.put("inputTuplesEmitted", 0);
                counts.put("inputTuplesDeduplicated", 0);
                return new Result(List.of(), Collections.unmodifiableMap(counts), List.copyOf(warnings));
            }
            inputCandidates.add(new ArrayList<>(sagaInputs));
        }

        inputCandidates.forEach(list -> list.sort(Comparator
                .comparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(InputVariant::sourceClassFqn, Comparator.nullsFirst(String::compareTo))
                .thenComparing(InputVariant::sourceMethodName, Comparator.nullsFirst(String::compareTo))));

        int[] counters = new int[2];
        buildTuples(orderedSagas, inputCandidates, 0, new ArrayList<>(), tuples, counters,
                conflictCandidates == null ? List.of() : conflictCandidates,
                sourceEvidence == null ? List.of() : sourceEvidence,
                mode == null ? InputTupleSelection.Mode.ALL : mode);

        List<InputTuple> orderedTuples = tuples.values().stream()
                .sorted(Comparator.comparing(InputTuple::signature))
                .toList();

        counts.put("inputTuplesSeen", counters[0]);
        counts.put("inputTuplesEmitted", orderedTuples.size());
        counts.put("inputTuplesDeduplicated", counters[1]);

        return new Result(List.copyOf(orderedTuples), Collections.unmodifiableMap(counts), List.copyOf(warnings));
    }

    /** Visits selected tuples one at a time without retaining the Cartesian result. */
    public static void forEachSelected(List<String> sagaOrder,
                                       Map<String, List<InputVariant>> inputsBySaga,
                                       List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates,
                                       List<SourceAggregateKeyInputEvidence> sourceEvidence,
                                       InputTupleSelection.Mode mode,
                                       Consumer<InputTuple> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        List<String> orderedSagas = sagaOrder == null ? List.of() : sagaOrder.stream()
                .filter(Objects::nonNull).distinct().sorted().toList();
        if (orderedSagas.isEmpty()) return;
        List<List<InputVariant>> inputCandidates = new ArrayList<>();
        for (String sagaFqn : orderedSagas) {
            List<InputVariant> sagaInputs = inputsBySaga == null ? null : inputsBySaga.get(sagaFqn);
            if (sagaInputs == null || sagaInputs.isEmpty()) return;
            List<InputVariant> sorted = new ArrayList<>(sagaInputs);
            sorted.sort(Comparator.comparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(InputVariant::sourceClassFqn, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(InputVariant::sourceMethodName, Comparator.nullsFirst(String::compareTo)));
            inputCandidates.add(sorted);
        }
        visitTuples(orderedSagas, inputCandidates, 0, new ArrayList<>(), new LinkedHashSet<>(),
                conflictCandidates == null ? List.of() : conflictCandidates,
                sourceEvidence == null ? List.of() : sourceEvidence,
                mode == null ? InputTupleSelection.Mode.ALL : mode, consumer);
    }

    private static void visitTuples(List<String> sagaOrder,
                                    List<List<InputVariant>> candidates,
                                    int index,
                                    List<InputVariant> current,
                                    Set<String> seenSignatures,
                                    List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates,
                                    List<SourceAggregateKeyInputEvidence> sourceEvidence,
                                    InputTupleSelection.Mode mode,
                                    Consumer<InputTuple> consumer) {
        if (index == candidates.size()) {
            if (!InputTupleSelection.selected(sagaOrder, current, conflictCandidates, sourceEvidence, mode)) return;
            String signature = signature(current);
            if (seenSignatures.add(signature)) {
                consumer.accept(new InputTuple(List.copyOf(current), signature, mergeWarnings(current)));
            }
            return;
        }
        if (!InputTupleSelection.potentiallySelected(sagaOrder, current, candidates,
                conflictCandidates, sourceEvidence, mode)) return;
        for (InputVariant candidate : candidates.get(index)) {
            current.add(candidate);
            visitTuples(sagaOrder, candidates, index + 1, current, seenSignatures,
                    conflictCandidates, sourceEvidence, mode, consumer);
            current.remove(current.size() - 1);
        }
    }

    private static void buildTuples(List<String> sagaOrder,
                                    List<List<InputVariant>> candidates,
                                    int index,
                                    List<InputVariant> current,
                                    LinkedHashMap<String, InputTuple> tuples,
                                    int[] counters,
                                    List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates,
                                    List<SourceAggregateKeyInputEvidence> sourceEvidence,
                                    InputTupleSelection.Mode mode) {
        if (index == candidates.size()) {
            counters[0]++;
            if (!InputTupleSelection.selected(sagaOrder, current, conflictCandidates, sourceEvidence, mode)) {
                return;
            }
            String signature = signature(current);
            InputTuple existing = tuples.get(signature);
            InputTuple tuple = new InputTuple(List.copyOf(current), signature, mergeWarnings(current));
            if (existing == null) {
                tuples.put(signature, tuple);
            } else {
                counters[1]++;
                tuples.put(signature, existing.withWarnings(mergeWarnings(existing.inputs(), current)));
            }
            return;
        }

        if (!InputTupleSelection.potentiallySelected(sagaOrder, current, candidates,
                conflictCandidates, sourceEvidence, mode)) {
            return;
        }

        List<InputVariant> sagaCandidates = candidates.get(index);
        for (InputVariant candidate : sagaCandidates) {
            current.add(candidate);
            buildTuples(sagaOrder, candidates, index + 1, current, tuples, counters,
                    conflictCandidates, sourceEvidence, mode);
            current.remove(current.size() - 1);
        }
    }

    private static String signature(List<InputVariant> inputs) {
        return inputs.stream()
                .map(input -> normalize(input.deterministicId()))
                .collect(java.util.stream.Collectors.joining("|"));
    }

    private static List<String> mergeWarnings(List<InputVariant> inputs) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (InputVariant input : inputs) {
            merged.addAll(input.warnings());
        }
        return List.copyOf(merged);
    }

    private static List<String> mergeWarnings(List<InputVariant> left, List<InputVariant> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (InputVariant input : left) {
            merged.addAll(input.warnings());
        }
        for (InputVariant input : right) {
            merged.addAll(input.warnings());
        }
        return List.copyOf(merged);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record InputTuple(List<InputVariant> inputs, String signature, List<String> warnings) {

        public InputTuple {
            inputs = inputs == null ? List.of() : List.copyOf(inputs);
            signature = signature == null || signature.isBlank() ? "" : signature;
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        public InputTuple withWarnings(List<String> newWarnings) {
            return new InputTuple(inputs, signature, newWarnings);
        }
    }

    public record Result(List<InputTuple> tuples, Map<String, Integer> counts, List<String> warnings) {
    }
}
