package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ConflictGraphBuilder.ConflictCandidate;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceValueReference;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceAggregateKeyInputEvidence;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared input-tuple semantics for catalog enumeration and grouped accounting. */
public final class InputTupleSelection {

    public enum Mode {
        ALL,
        STRICT,
        WITH_TYPE_ONLY_FALLBACK
    }

    private InputTupleSelection() {
    }

    public static boolean selected(List<String> sagaOrder,
                                   List<InputVariant> inputs,
                                   List<ConflictCandidate> candidates,
                                   List<SourceAggregateKeyInputEvidence> sourceEvidence,
                                   Mode mode) {
        List<String> sagas = orderedSagas(sagaOrder);
        if (sagas.size() != (inputs == null ? 0 : inputs.size())) {
            return false;
        }
        LinkedHashMap<String, EvidenceProfile> profiles = new LinkedHashMap<>();
        EvidenceIndex evidenceIndex = new EvidenceIndex(sourceEvidence);
        for (int index = 0; index < sagas.size(); index++) {
            InputVariant input = inputs.get(index);
            if (input == null || !Objects.equals(sagas.get(index), input.sagaFqn())) {
                return false;
            }
            profiles.put(sagas.get(index), evidenceIndex.profile(input));
        }
        return selectedProfiles(sagas, profiles, candidates, mode);
    }

    public static BigInteger count(List<String> sagaOrder,
                                   Map<String, List<InputVariant>> inputsBySaga,
                                   List<ConflictCandidate> candidates,
                                   List<SourceAggregateKeyInputEvidence> sourceEvidence,
                                   Mode mode) {
        List<String> sagas = orderedSagas(sagaOrder);
        if (sagas.isEmpty()) {
            return BigInteger.ZERO;
        }
        if (mode == Mode.ALL) {
            return cartesianCount(sagas, inputsBySaga);
        }
        List<ConflictCandidate> safeCandidates = candidates == null ? List.of() : candidates;
        if (!potentiallyConnected(sagas, safeCandidates)) {
            return BigInteger.ZERO;
        }
        if (mode == Mode.WITH_TYPE_ONLY_FALLBACK && hasNoExactBindings(sagas, inputsBySaga)) {
            return cartesianCount(sagas, inputsBySaga);
        }

        EvidenceIndex evidenceIndex = new EvidenceIndex(sourceEvidence);
        List<List<ProfileGroup>> groupsBySaga = new ArrayList<>();
        for (String saga : sagas) {
            List<InputVariant> inputs = inputsBySaga == null ? null : inputsBySaga.get(saga);
            if (inputs == null || inputs.isEmpty()) {
                return BigInteger.ZERO;
            }
            LinkedHashMap<EvidenceProfile, BigInteger> counts = new LinkedHashMap<>();
            inputs.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(InputVariant::deterministicId,
                            Comparator.nullsFirst(String::compareTo)))
                    .forEach(input -> counts.merge(evidenceIndex.profile(input), BigInteger.ONE, BigInteger::add));
            groupsBySaga.add(counts.entrySet().stream()
                    .map(entry -> new ProfileGroup(entry.getKey(), entry.getValue()))
                    .toList());
        }
        return countGroups(sagas, groupsBySaga, 0, new LinkedHashMap<>(), BigInteger.ONE,
                safeCandidates, mode);
    }

    private static BigInteger cartesianCount(List<String> sagas,
                                             Map<String, List<InputVariant>> inputsBySaga) {
        BigInteger product = BigInteger.ONE;
        for (String saga : sagas) {
            List<InputVariant> inputs = inputsBySaga == null ? null : inputsBySaga.get(saga);
            if (inputs == null || inputs.isEmpty()) {
                return BigInteger.ZERO;
            }
            product = product.multiply(BigInteger.valueOf(inputs.size()));
        }
        return product;
    }

    private static boolean hasNoExactBindings(List<String> sagas,
                                              Map<String, List<InputVariant>> inputsBySaga) {
        for (String saga : sagas) {
            List<InputVariant> inputs = inputsBySaga == null ? null : inputsBySaga.get(saga);
            if (inputs == null || inputs.isEmpty()) {
                return false;
            }
            for (InputVariant input : inputs) {
                if (input != null && input.logicalKeyBindings() != null
                        && !input.logicalKeyBindings().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<ConflictCandidate> selectedCandidates(List<String> sagaOrder,
                                                              List<InputVariant> inputs,
                                                              List<ConflictCandidate> candidates,
                                                              List<SourceAggregateKeyInputEvidence> sourceEvidence,
                                                              Mode mode) {
        List<String> sagas = orderedSagas(sagaOrder);
        if (sagas.size() != (inputs == null ? 0 : inputs.size())) {
            return List.of();
        }
        EvidenceIndex evidenceIndex = new EvidenceIndex(sourceEvidence);
        LinkedHashMap<String, EvidenceProfile> profiles = new LinkedHashMap<>();
        for (int index = 0; index < sagas.size(); index++) {
            InputVariant input = inputs.get(index);
            if (input == null || !Objects.equals(sagas.get(index), input.sagaFqn())) {
                return List.of();
            }
            profiles.put(sagas.get(index), evidenceIndex.profile(input));
        }
        Set<String> sagaSet = new LinkedHashSet<>(sagas);
        return (candidates == null ? List.<ConflictCandidate>of() : candidates).stream()
                .filter(candidate -> sagaSet.contains(candidate.leftSagaFqn())
                        && sagaSet.contains(candidate.rightSagaFqn()))
                .filter(candidate -> mode == Mode.ALL
                        || candidateSelected(candidate, profiles.get(candidate.leftSagaFqn()),
                        profiles.get(candidate.rightSagaFqn()), mode))
                .toList();
    }

    private static BigInteger countGroups(List<String> sagas,
                                          List<List<ProfileGroup>> groupsBySaga,
                                          int index,
                                          LinkedHashMap<String, EvidenceProfile> selected,
                                          BigInteger multiplicity,
                                          List<ConflictCandidate> candidates,
                                          Mode mode) {
        if (index == sagas.size()) {
            return selectedProfiles(sagas, selected, candidates, mode) ? multiplicity : BigInteger.ZERO;
        }
        BigInteger count = BigInteger.ZERO;
        String saga = sagas.get(index);
        for (ProfileGroup group : groupsBySaga.get(index)) {
            selected.put(saga, group.profile());
            count = count.add(countGroups(sagas, groupsBySaga, index + 1, selected,
                    multiplicity.multiply(group.count()), candidates, mode));
            selected.remove(saga);
        }
        return count;
    }

    private static boolean selectedProfiles(List<String> sagas,
                                            Map<String, EvidenceProfile> profiles,
                                            List<ConflictCandidate> candidates,
                                            Mode mode) {
        if (mode == Mode.ALL || sagas.size() == 1) {
            return true;
        }
        LinkedHashMap<String, LinkedHashSet<String>> adjacency = new LinkedHashMap<>();
        Set<String> sagaSet = new LinkedHashSet<>(sagas);
        for (ConflictCandidate candidate : candidates == null ? List.<ConflictCandidate>of() : candidates) {
            if (!sagaSet.contains(candidate.leftSagaFqn()) || !sagaSet.contains(candidate.rightSagaFqn())) {
                continue;
            }
            EvidenceProfile left = profiles.get(candidate.leftSagaFqn());
            EvidenceProfile right = profiles.get(candidate.rightSagaFqn());
            if (left == null || right == null || !candidateSelected(candidate, left, right, mode)) {
                continue;
            }
            adjacency.computeIfAbsent(candidate.leftSagaFqn(), ignored -> new LinkedHashSet<>())
                    .add(candidate.rightSagaFqn());
            adjacency.computeIfAbsent(candidate.rightSagaFqn(), ignored -> new LinkedHashSet<>())
                    .add(candidate.leftSagaFqn());
        }
        ArrayDeque<String> pending = new ArrayDeque<>();
        LinkedHashSet<String> reached = new LinkedHashSet<>();
        pending.add(sagas.get(0));
        while (!pending.isEmpty()) {
            String saga = pending.removeFirst();
            if (!reached.add(saga)) {
                continue;
            }
            adjacency.getOrDefault(saga, new LinkedHashSet<>()).stream()
                    .filter(next -> !reached.contains(next))
                    .forEach(pending::addLast);
        }
        return reached.size() == sagas.size();
    }

    private static boolean potentiallyConnected(List<String> sagas, List<ConflictCandidate> candidates) {
        if (sagas.size() <= 1) {
            return true;
        }
        Set<String> sagaSet = new LinkedHashSet<>(sagas);
        LinkedHashMap<String, LinkedHashSet<String>> adjacency = new LinkedHashMap<>();
        for (ConflictCandidate candidate : candidates) {
            if (!sagaSet.contains(candidate.leftSagaFqn()) || !sagaSet.contains(candidate.rightSagaFqn())) {
                continue;
            }
            adjacency.computeIfAbsent(candidate.leftSagaFqn(), ignored -> new LinkedHashSet<>())
                    .add(candidate.rightSagaFqn());
            adjacency.computeIfAbsent(candidate.rightSagaFqn(), ignored -> new LinkedHashSet<>())
                    .add(candidate.leftSagaFqn());
        }
        ArrayDeque<String> pending = new ArrayDeque<>();
        LinkedHashSet<String> reached = new LinkedHashSet<>();
        pending.add(sagas.get(0));
        while (!pending.isEmpty()) {
            String saga = pending.removeFirst();
            if (!reached.add(saga)) {
                continue;
            }
            adjacency.getOrDefault(saga, new LinkedHashSet<>()).stream()
                    .filter(next -> !reached.contains(next))
                    .forEach(pending::addLast);
        }
        return reached.size() == sagas.size();
    }

    private static boolean candidateSelected(ConflictCandidate candidate,
                                             EvidenceProfile leftProfile,
                                             EvidenceProfile rightProfile,
                                             Mode mode) {
        KeyRelation relation = relation(candidate.leftFootprint(), leftProfile,
                candidate.rightFootprint(), rightProfile);
        return mode == Mode.STRICT ? relation == KeyRelation.POSITIVE : relation != KeyRelation.UNEQUAL;
    }

    private static KeyRelation relation(StepFootprint leftFootprint,
                                        EvidenceProfile leftProfile,
                                        StepFootprint rightFootprint,
                                        EvidenceProfile rightProfile) {
        AggregateKey leftKey = leftFootprint == null ? null : leftFootprint.aggregateKey();
        AggregateKey rightKey = rightFootprint == null ? null : rightFootprint.aggregateKey();
        if (!resolved(leftKey) || !resolved(rightKey)) {
            return KeyRelation.MISSING;
        }

        Set<String> leftExact = exactValues(leftKey, leftProfile);
        Set<String> rightExact = exactValues(rightKey, rightProfile);
        if (!leftExact.isEmpty() && !rightExact.isEmpty()) {
            if (leftExact.stream().anyMatch(rightExact::contains)) {
                return KeyRelation.POSITIVE;
            }
            return KeyRelation.UNEQUAL;
        }

        Set<String> leftSources = leftKey.confidence() == FootprintConfidence.SYMBOLIC
                ? leftProfile.sources(leftKey) : Set.of();
        Set<String> rightSources = rightKey.confidence() == FootprintConfidence.SYMBOLIC
                ? rightProfile.sources(rightKey) : Set.of();
        if (!leftSources.isEmpty() && !rightSources.isEmpty()
                && leftSources.stream().anyMatch(rightSources::contains)) {
            return KeyRelation.POSITIVE;
        }
        return KeyRelation.MISSING;
    }

    private static Set<String> exactValues(AggregateKey key, EvidenceProfile profile) {
        if (key.confidence() == FootprintConfidence.EXACT) {
            String exact = ExactKeyValueNormalizer.normalize(key.keyText());
            return exact == null ? Set.of() : Set.of(exact);
        }
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        aliases.add(normalizeIdentifier(key.keyText()));
        String aggregate = normalizeIdentifier(key.aggregateName());
        if (aggregate != null) {
            aliases.add(aggregate + "id");
            aliases.add(aggregate + "aggregateid");
        }
        aliases.add("aggregateid");
        aliases.remove(null);

        LinkedHashSet<String> values = new LinkedHashSet<>();
        profile.exactBindings().forEach((binding, value) -> {
            if (aliases.contains(normalizeIdentifier(binding))) {
                String normalizedValue = ExactKeyValueNormalizer.normalize(value);
                if (normalizedValue != null) {
                    values.add(normalizedValue);
                }
            }
        });
        return Set.copyOf(values);
    }

    private static boolean resolved(AggregateKey key) {
        return key != null
                && normalize(key.keyText()) != null
                && key.confidence() != FootprintConfidence.TYPE_ONLY
                && key.confidence() != FootprintConfidence.UNKNOWN;
    }

    private static List<String> orderedSagas(List<String> sagaOrder) {
        return sagaOrder == null ? List.of() : sagaOrder.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private static String canonicalSource(GroovySourceValueReference reference) {
        if (reference == null || normalize(reference.occurrenceId()) == null) {
            return null;
        }
        return String.join("\u0000",
                String.valueOf(reference.occurrenceId()),
                String.valueOf(reference.producerMethodName()),
                String.join(".", reference.propertyPath()));
    }

    private static String normalizeIdentifier(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        int separator = Math.max(normalized.lastIndexOf('.'), normalized.lastIndexOf(':'));
        String leaf = separator < 0 ? normalized : normalized.substring(separator + 1);
        if (leaf.startsWith("get") && leaf.endsWith("()") && leaf.length() > 5) {
            leaf = leaf.substring(3, leaf.length() - 2);
        }
        return leaf.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private enum KeyRelation {
        POSITIVE,
        MISSING,
        UNEQUAL
    }

    private record ProfileGroup(EvidenceProfile profile, BigInteger count) {
    }

    private record EvidenceProfile(Map<String, String> exactBindings,
                                   Map<KeyEvidenceIdentity, Set<String>> sourcesByKey) {
        EvidenceProfile {
            exactBindings = exactBindings == null ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(exactBindings));
            if (sourcesByKey == null || sourcesByKey.isEmpty()) {
                sourcesByKey = Map.of();
            } else {
                LinkedHashMap<KeyEvidenceIdentity, Set<String>> copy = new LinkedHashMap<>();
                sourcesByKey.forEach((key, sources) -> copy.put(key, Set.copyOf(sources)));
                sourcesByKey = Collections.unmodifiableMap(copy);
            }
        }

        Set<String> sources(AggregateKey key) {
            KeyEvidenceIdentity identity = KeyEvidenceIdentity.of(key);
            return identity == null ? Set.of() : sourcesByKey.getOrDefault(identity, Set.of());
        }
    }

    private record KeyEvidenceIdentity(String aggregateName,
                                       int constructorArgumentIndex,
                                       List<String> propertyPath) {
        static KeyEvidenceIdentity of(AggregateKey key) {
            if (key == null || normalize(key.aggregateName()) == null
                    || key.sourceConstructorArgumentIndex() == null) {
                return null;
            }
            return new KeyEvidenceIdentity(normalize(key.aggregateName()),
                    key.sourceConstructorArgumentIndex(), normalizePath(key.sourcePropertyPath()));
        }

        static KeyEvidenceIdentity of(SourceAggregateKeyInputEvidence evidence) {
            if (evidence == null || normalize(evidence.aggregateName()) == null) {
                return null;
            }
            return new KeyEvidenceIdentity(normalize(evidence.aggregateName()),
                    evidence.constructorArgumentIndex(), normalizePath(evidence.aggregateKeyPropertyPath()));
        }

        private static List<String> normalizePath(List<String> path) {
            return path == null ? List.of() : path.stream()
                    .map(InputTupleSelection::normalizeIdentifier)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private static final class EvidenceIndex {
        private final Map<String, List<SourceAggregateKeyInputEvidence>> byInputId;

        private EvidenceIndex(List<SourceAggregateKeyInputEvidence> evidence) {
            LinkedHashMap<String, List<SourceAggregateKeyInputEvidence>> grouped = new LinkedHashMap<>();
            for (SourceAggregateKeyInputEvidence item : evidence == null
                    ? List.<SourceAggregateKeyInputEvidence>of() : evidence) {
                if (item == null || item.producerReference() == null
                        || normalize(item.inputVariantId()) == null
                        || KeyEvidenceIdentity.of(item) == null) {
                    continue;
                }
                grouped.computeIfAbsent(item.inputVariantId(), ignored -> new ArrayList<>()).add(item);
            }
            LinkedHashMap<String, List<SourceAggregateKeyInputEvidence>> stable = new LinkedHashMap<>();
            grouped.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> stable.put(entry.getKey(), List.copyOf(entry.getValue())));
            byInputId = Collections.unmodifiableMap(stable);
        }

        private EvidenceProfile profile(InputVariant input) {
            LinkedHashMap<String, String> exact = new LinkedHashMap<>();
            if (input.logicalKeyBindings() != null) {
                input.logicalKeyBindings().entrySet().stream()
                        .filter(entry -> normalize(entry.getKey()) != null && normalize(entry.getValue()) != null)
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> exact.put(entry.getKey().trim(), entry.getValue().trim()));
            }
            LinkedHashMap<KeyEvidenceIdentity, LinkedHashSet<String>> sources = new LinkedHashMap<>();
            for (SourceAggregateKeyInputEvidence item : byInputId.getOrDefault(input.deterministicId(), List.of())) {
                KeyEvidenceIdentity key = KeyEvidenceIdentity.of(item);
                String source = canonicalSource(item.producerReference());
                if (key != null && source != null) {
                    sources.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(source);
                }
            }
            LinkedHashMap<KeyEvidenceIdentity, Set<String>> immutableSources = new LinkedHashMap<>();
            sources.forEach((key, value) -> immutableSources.put(key, Set.copyOf(value)));
            return new EvidenceProfile(exact, immutableSources);
        }
    }
}
