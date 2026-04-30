package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConflictGraphBuilder {

    private ConflictGraphBuilder() {
    }

    public static Result build(List<SagaDefinition> sagaDefinitions, ScenarioGeneratorConfig config) {
        ScenarioGeneratorConfig effectiveConfig = config == null ? new ScenarioGeneratorConfig() : config;
        List<SagaDefinition> safeSagas = sagaDefinitions == null ? List.of() : sagaDefinitions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SagaDefinition::sagaFqn, Comparator.nullsFirst(String::compareTo)))
                .toList();

        LinkedHashMap<String, LinkedHashSet<String>> adjacency = new LinkedHashMap<>();
        LinkedHashMap<String, ConflictCandidate> candidates = new LinkedHashMap<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

        int footprintPairsSeen = 0;
        int conflictsEmitted = 0;
        int readReadIgnored = 0;
        int rejected = 0;
        int typeOnlyFallbackEdges = 0;
        int symbolicFallbackEdges = 0;
        int unknownFallbackEdges = 0;

        for (int leftSagaIndex = 0; leftSagaIndex < safeSagas.size(); leftSagaIndex++) {
            SagaDefinition leftSaga = safeSagas.get(leftSagaIndex);
            for (int rightSagaIndex = leftSagaIndex + 1; rightSagaIndex < safeSagas.size(); rightSagaIndex++) {
                SagaDefinition rightSaga = safeSagas.get(rightSagaIndex);
                for (StepDefinition leftStep : sortedSteps(leftSaga)) {
                    for (StepDefinition rightStep : sortedSteps(rightSaga)) {
                        for (StepFootprint leftFootprint : leftStep.footprints()) {
                            for (StepFootprint rightFootprint : rightStep.footprints()) {
                                footprintPairsSeen++;
                                MatchResult matchResult = match(leftFootprint, rightFootprint, effectiveConfig.allowTypeOnlyFallback());
                                if (!matchResult.matched()) {
                                    rejected++;
                                    continue;
                                }

                                if (leftFootprint.accessMode() == AccessMode.READ && rightFootprint.accessMode() == AccessMode.READ) {
                                    readReadIgnored++;
                                    continue;
                                }

                                ConflictKind kind = matchResult.kind(leftFootprint.accessMode(), rightFootprint.accessMode());
                                String leftStepId = ScenarioIdGenerator.stepDefinitionId(leftSaga.sagaFqn(), leftStep);
                                String rightStepId = ScenarioIdGenerator.stepDefinitionId(rightSaga.sagaFqn(), rightStep);
                                String conflictId = ScenarioIdGenerator.conflictEvidenceId(
                                        leftStepId,
                                        rightStepId,
                                        leftFootprint.aggregateKey(),
                                        rightFootprint.aggregateKey(),
                                        leftFootprint.accessMode(),
                                        rightFootprint.accessMode(),
                                        kind);

                                List<String> candidateWarnings = new ArrayList<>(matchResult.warnings());
                                candidateWarnings.addAll(leftStep.warnings());
                                candidateWarnings.addAll(rightStep.warnings());
                                candidateWarnings.addAll(leftFootprint.warnings());
                                candidateWarnings.addAll(rightFootprint.warnings());

                                ConflictCandidate existing = candidates.get(conflictId);
                                if (existing == null) {
                                    ConflictCandidate candidate = new ConflictCandidate(
                                            conflictId,
                                            leftSaga.sagaFqn(),
                                            rightSaga.sagaFqn(),
                                            leftStep,
                                            rightStep,
                                            leftFootprint,
                                            rightFootprint,
                                            leftStepId,
                                            rightStepId,
                                            kind,
                                            matchResult.fallbackUsed(),
                                            List.copyOf(candidateWarnings));
                                    candidates.put(conflictId, candidate);
                                    adjacency.computeIfAbsent(leftSaga.sagaFqn(), ignored -> new LinkedHashSet<>()).add(rightSaga.sagaFqn());
                                    adjacency.computeIfAbsent(rightSaga.sagaFqn(), ignored -> new LinkedHashSet<>()).add(leftSaga.sagaFqn());
                                    conflictsEmitted++;
                                    if (matchResult.fallbackUsed()) {
                                        switch (matchResult.fallbackKind()) {
                                            case TYPE_ONLY -> typeOnlyFallbackEdges++;
                                            case SYMBOLIC -> symbolicFallbackEdges++;
                                            case UNKNOWN -> unknownFallbackEdges++;
                                            default -> {
                                            }
                                        }
                                        warnings.add(matchResult.fallbackWarning());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        counts.put("footprintPairsSeen", footprintPairsSeen);
        counts.put("conflictEdgesEmitted", conflictsEmitted);
        counts.put("conflictEdgesIgnoredReadRead", readReadIgnored);
        counts.put("conflictEdgesRejected", rejected);
        counts.put("typeOnlyFallbackEdges", typeOnlyFallbackEdges);
        counts.put("symbolicFallbackEdges", symbolicFallbackEdges);
        counts.put("unknownFallbackEdges", unknownFallbackEdges);

        Map<String, Set<String>> immutableAdjacency = new LinkedHashMap<>();
        adjacency.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> immutableAdjacency.put(entry.getKey(), Set.copyOf(entry.getValue())));

        List<ConflictCandidate> orderedCandidates = candidates.values().stream()
                .sorted(Comparator
                        .comparing(ConflictCandidate::leftSagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::rightSagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::leftStepId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::rightStepId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();

        return new Result(Collections.unmodifiableMap(immutableAdjacency), List.copyOf(orderedCandidates), Collections.unmodifiableMap(counts), List.copyOf(warnings));
    }

    private static List<StepDefinition> sortedSteps(SagaDefinition sagaDefinition) {
        return sagaDefinition.steps().stream()
                .sorted(Comparator
                        .comparingInt(StepDefinition::orderIndex)
                        .thenComparing(StepDefinition::deterministicId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(StepDefinition::stepKey, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(StepDefinition::name, Comparator.nullsFirst(String::compareTo)))
                .toList();
    }

    private static MatchResult match(StepFootprint leftFootprint, StepFootprint rightFootprint, boolean allowTypeOnlyFallback) {
        AggregateKey leftKey = leftFootprint.aggregateKey();
        AggregateKey rightKey = rightFootprint.aggregateKey();
        if (leftKey == null || rightKey == null) {
            return MatchResult.noMatch();
        }

        if (!sameAggregateIdentity(leftKey, rightKey)) {
            return MatchResult.noMatch();
        }

        String leftExactKey = normalize(leftKey.keyText());
        String rightExactKey = normalize(rightKey.keyText());
        boolean bothExact = leftKey.confidence() == FootprintConfidence.EXACT && rightKey.confidence() == FootprintConfidence.EXACT;
        boolean sameKeyText = leftExactKey != null && leftExactKey.equals(rightExactKey);

        if (bothExact) {
            if (sameKeyText) {
                return MatchResult.exact();
            }
            if (leftExactKey != null && rightExactKey != null) {
                return MatchResult.noMatch();
            }
            if (allowTypeOnlyFallback) {
                return MatchResult.fallback(FootprintConfidence.TYPE_ONLY, "type-only fallback used for aggregate " + aggregateLabel(leftKey));
            }
            return MatchResult.noMatch();
        }

        if (sameKeyText) {
            if (leftKey.confidence() == FootprintConfidence.SYMBOLIC || rightKey.confidence() == FootprintConfidence.SYMBOLIC) {
                return MatchResult.symbolic("symbolic aggregate match used for aggregate " + aggregateLabel(leftKey));
            }
            if (leftKey.confidence() == FootprintConfidence.TYPE_ONLY || rightKey.confidence() == FootprintConfidence.TYPE_ONLY) {
                if (allowTypeOnlyFallback) {
                    return MatchResult.fallback(FootprintConfidence.TYPE_ONLY, "type-only fallback used for aggregate " + aggregateLabel(leftKey));
                }
                return MatchResult.noMatch();
            }
            if (leftKey.confidence() == FootprintConfidence.UNKNOWN || rightKey.confidence() == FootprintConfidence.UNKNOWN) {
                if (allowTypeOnlyFallback) {
                    return MatchResult.fallback(FootprintConfidence.UNKNOWN, "unknown-confidence fallback used for aggregate " + aggregateLabel(leftKey));
                }
                return MatchResult.noMatch();
            }
            return MatchResult.exact();
        }

        if (leftExactKey != null && rightExactKey != null) {
            return MatchResult.noMatch();
        }

        if (leftKey.confidence() == FootprintConfidence.SYMBOLIC || rightKey.confidence() == FootprintConfidence.SYMBOLIC) {
            if (sameAggregateIdentity(leftKey, rightKey)) {
                return MatchResult.symbolic("symbolic aggregate match used for aggregate " + aggregateLabel(leftKey));
            }
        }

        if (allowTypeOnlyFallback) {
            if (leftKey.confidence() == FootprintConfidence.TYPE_ONLY || rightKey.confidence() == FootprintConfidence.TYPE_ONLY) {
                return MatchResult.fallback(FootprintConfidence.TYPE_ONLY, "type-only fallback used for aggregate " + aggregateLabel(leftKey));
            }
            if (leftKey.confidence() == FootprintConfidence.UNKNOWN || rightKey.confidence() == FootprintConfidence.UNKNOWN) {
                return MatchResult.fallback(FootprintConfidence.UNKNOWN, "unknown-confidence fallback used for aggregate " + aggregateLabel(leftKey));
            }
        }

        return MatchResult.noMatch();
    }

    private static boolean sameAggregateIdentity(AggregateKey leftKey, AggregateKey rightKey) {
        return Objects.equals(normalize(leftKey.aggregateTypeName()), normalize(rightKey.aggregateTypeName()))
                && Objects.equals(normalize(leftKey.aggregateName()), normalize(rightKey.aggregateName()));
    }

    private static String aggregateLabel(AggregateKey aggregateKey) {
        return (normalize(aggregateKey.aggregateTypeName()) == null ? "?" : normalize(aggregateKey.aggregateTypeName()))
                + "/"
                + (normalize(aggregateKey.aggregateName()) == null ? "?" : normalize(aggregateKey.aggregateName()));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record MatchResult(boolean matched,
                               boolean fallbackUsed,
                               FootprintConfidence fallbackKind,
                               String fallbackWarning,
                               List<String> warnings) {

        static MatchResult noMatch() {
            return new MatchResult(false, false, null, null, List.of());
        }

        static MatchResult exact() {
            return new MatchResult(true, false, null, null, List.of());
        }

        static MatchResult symbolic(String warning) {
            return new MatchResult(true, true, FootprintConfidence.SYMBOLIC, warning, List.of(warning));
        }

        static MatchResult fallback(FootprintConfidence kind, String warning) {
            return new MatchResult(true, true, kind == null ? FootprintConfidence.UNKNOWN : kind, warning, List.of(warning));
        }

        ConflictKind kind(AccessMode leftAccessMode, AccessMode rightAccessMode) {
            if (!matched) {
                return ConflictKind.UNKNOWN;
            }
            if (fallbackUsed) {
                return switch (fallbackKind == null ? FootprintConfidence.UNKNOWN : fallbackKind) {
                    case TYPE_ONLY -> ConflictKind.TYPE_ONLY;
                    case SYMBOLIC -> ConflictKind.SYMBOLIC;
                    case UNKNOWN -> ConflictKind.UNKNOWN;
                    case EXACT -> accessKind(leftAccessMode, rightAccessMode);
                };
            }
            return accessKind(leftAccessMode, rightAccessMode);
        }

        private ConflictKind accessKind(AccessMode leftAccessMode, AccessMode rightAccessMode) {
            if (leftAccessMode == AccessMode.WRITE && rightAccessMode == AccessMode.WRITE) {
                return ConflictKind.WRITE_WRITE;
            }
            if (leftAccessMode == AccessMode.WRITE && rightAccessMode == AccessMode.READ) {
                return ConflictKind.WRITE_READ;
            }
            if (leftAccessMode == AccessMode.READ && rightAccessMode == AccessMode.WRITE) {
                return ConflictKind.READ_WRITE;
            }
            return ConflictKind.UNKNOWN;
        }
    }

    public record ConflictCandidate(
            String deterministicId,
            String leftSagaFqn,
            String rightSagaFqn,
            StepDefinition leftStep,
            StepDefinition rightStep,
            StepFootprint leftFootprint,
            StepFootprint rightFootprint,
            String leftStepId,
            String rightStepId,
            ConflictKind kind,
            boolean fallbackUsed,
            List<String> warnings) {
    }

    public record Result(
            Map<String, Set<String>> adjacency,
            List<ConflictCandidate> conflictCandidates,
            Map<String, Integer> counts,
            List<String> warnings) {
    }
}
