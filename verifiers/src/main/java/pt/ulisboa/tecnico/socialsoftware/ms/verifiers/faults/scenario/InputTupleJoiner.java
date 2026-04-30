package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class InputTupleJoiner {

    private InputTupleJoiner() {
    }

    public static Result join(List<String> sagaOrder, Map<String, List<InputVariant>> inputsBySaga) {
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

        List<List<InputVariant>> candidates = new ArrayList<>();
        for (String sagaFqn : orderedSagas) {
            List<InputVariant> sagaInputs = inputsBySaga == null ? null : inputsBySaga.get(sagaFqn);
            if (sagaInputs == null || sagaInputs.isEmpty()) {
                warnings.add("no usable inputs for saga " + sagaFqn);
                counts.put("inputTuplesSeen", 0);
                counts.put("inputTuplesEmitted", 0);
                counts.put("inputTuplesDeduplicated", 0);
                return new Result(List.of(), Collections.unmodifiableMap(counts), List.copyOf(warnings));
            }
            candidates.add(new ArrayList<>(sagaInputs));
        }

        candidates.forEach(list -> list.sort(Comparator
                .comparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo))
                .thenComparing(InputVariant::sourceClassFqn, Comparator.nullsFirst(String::compareTo))
                .thenComparing(InputVariant::sourceMethodName, Comparator.nullsFirst(String::compareTo))));

        int[] counters = new int[2];
        buildTuples(candidates, 0, new ArrayList<>(), tuples, counters);

        List<InputTuple> orderedTuples = tuples.values().stream()
                .sorted(Comparator.comparing(InputTuple::signature))
                .toList();

        counts.put("inputTuplesSeen", counters[0]);
        counts.put("inputTuplesEmitted", orderedTuples.size());
        counts.put("inputTuplesDeduplicated", counters[1]);

        return new Result(List.copyOf(orderedTuples), Collections.unmodifiableMap(counts), List.copyOf(warnings));
    }

    private static void buildTuples(List<List<InputVariant>> candidates,
                                    int index,
                                    List<InputVariant> current,
                                    LinkedHashMap<String, InputTuple> tuples,
                                    int[] counters) {
        if (index == candidates.size()) {
            counters[0]++;
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

        List<InputVariant> sagaCandidates = candidates.get(index);
        for (InputVariant candidate : sagaCandidates) {
            if (!compatible(current, candidate)) {
                continue;
            }
            current.add(candidate);
            buildTuples(candidates, index + 1, current, tuples, counters);
            current.remove(current.size() - 1);
        }
    }

    private static boolean compatible(List<InputVariant> current, InputVariant candidate) {
        Map<String, String> candidateBindings = knownExactBindings(candidate);
        if (candidateBindings.isEmpty()) {
            return true;
        }

        for (InputVariant existing : current) {
            Map<String, String> existingBindings = knownExactBindings(existing);
            if (existingBindings.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, String> candidateEntry : candidateBindings.entrySet()) {
                String bindingKey = candidateEntry.getKey();
                String candidateValue = candidateEntry.getValue();
                String existingValue = existingBindings.get(bindingKey);
                if (existingValue == null || candidateValue == null) {
                    continue;
                }
                if (!existingValue.equals(candidateValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static Map<String, String> knownExactBindings(InputVariant inputVariant) {
        LinkedHashMap<String, String> bindings = new LinkedHashMap<>();
        if (inputVariant == null || inputVariant.logicalKeyBindings() == null || inputVariant.logicalKeyBindings().isEmpty()) {
            return bindings;
        }

        for (Map.Entry<String, String> entry : inputVariant.logicalKeyBindings().entrySet()) {
            String bindingKey = normalize(entry.getKey());
            String bindingValue = normalize(entry.getValue());
            if (bindingKey == null || bindingValue == null) {
                continue;
            }
            bindings.put(bindingKey, bindingValue);
        }

        return bindings;
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
