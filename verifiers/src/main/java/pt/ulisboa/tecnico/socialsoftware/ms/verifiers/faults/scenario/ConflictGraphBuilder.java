package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition;
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
                                            ConflictOrigin.FORWARD,
                                            true,
                                            true,
                                            null,
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

    /**
     * Builds the graph used to select and schedule workload participants.  The
     * ordinary {@link #build(List, ScenarioGeneratorConfig)} graph remains the
     * forward-only projection used by direct-interaction artifacts.
     */
    public static Result buildSelectionGraph(List<SagaDefinition> sagaDefinitions,
                                             List<EventConsequenceDefinition> eventDefinitions,
                                             ScenarioGeneratorConfig config) {
        ScenarioGeneratorConfig effectiveConfig = config == null ? new ScenarioGeneratorConfig() : config;
        List<SagaDefinition> safeSagas = sagaDefinitions == null ? List.of() : sagaDefinitions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SagaDefinition::sagaFqn, Comparator.nullsFirst(String::compareTo)))
                .toList();
        Map<String, SagaDefinition> sagaByFqn = new LinkedHashMap<>();
        safeSagas.forEach(saga -> sagaByFqn.putIfAbsent(saga.sagaFqn(), saga));

        Result forward = build(safeSagas, effectiveConfig);
        LinkedHashMap<String, LinkedHashSet<String>> adjacency = mutableAdjacency(forward.adjacency());
        LinkedHashMap<String, ConflictCandidate> candidates = new LinkedHashMap<>();
        forward.conflictCandidates().forEach(candidate -> candidates.put(candidate.deterministicId(), candidate));
        LinkedHashSet<String> warnings = new LinkedHashSet<>(forward.warnings());
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>(forward.counts());
        int recoveryCandidates = 0;
        int eventCandidates = 0;

        for (int leftSagaIndex = 0; leftSagaIndex < safeSagas.size(); leftSagaIndex++) {
            SagaDefinition leftSaga = safeSagas.get(leftSagaIndex);
            for (int rightSagaIndex = leftSagaIndex + 1; rightSagaIndex < safeSagas.size(); rightSagaIndex++) {
                SagaDefinition rightSaga = safeSagas.get(rightSagaIndex);
                for (StepDefinition leftStep : sortedSteps(leftSaga)) {
                    for (StepDefinition rightStep : sortedSteps(rightSaga)) {
                        recoveryCandidates += addRecoveryCandidates(candidates, adjacency, warnings,
                                leftSaga, leftStep, rightSaga, rightStep, effectiveConfig);
                    }
                }
            }
        }

        List<EventConsequenceDefinition> events = eventDefinitions == null ? List.of() : eventDefinitions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(EventConsequenceDefinition::triggerSagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::triggerStepKey, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(definition -> definition.emissionSite() == null ? null
                                : definition.emissionSite().deterministicId(), Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::downstreamSagaFqn, Comparator.nullsFirst(String::compareTo)))
                .toList();
        for (EventConsequenceDefinition event : events) {
            SagaDefinition triggerSaga = sagaByFqn.get(event.triggerSagaFqn());
            SagaDefinition downstreamSaga = sagaByFqn.get(event.downstreamSagaFqn());
            StepDefinition triggerStep = findTriggerStep(triggerSaga, event.triggerStepKey());
            if (triggerSaga == null || downstreamSaga == null || triggerStep == null || event.emissionSite() == null) {
                continue;
            }
            for (SagaDefinition selectedSaga : safeSagas) {
                if (Objects.equals(selectedSaga.sagaFqn(), triggerSaga.sagaFqn())
                        || Objects.equals(selectedSaga.sagaFqn(), downstreamSaga.sagaFqn())) {
                    continue;
                }
                for (StepDefinition downstreamStep : sortedSteps(downstreamSaga)) {
                    for (StepDefinition selectedStep : sortedSteps(selectedSaga)) {
                        for (StepFootprint downstreamFootprint : downstreamStep.footprints()) {
                            for (StepFootprint selectedFootprint : allAccessFootprints(selectedStep)) {
                                MatchResult match = match(downstreamFootprint, selectedFootprint,
                                        effectiveConfig.allowTypeOnlyFallback());
                                if (!match.matched() || readRead(downstreamFootprint, selectedFootprint)) {
                                    continue;
                                }
                                List<String> candidateWarnings = new ArrayList<>(match.warnings());
                                candidateWarnings.add("event-mediated selection edge; receiver identity is not attributed to the producer input");
                                candidateWarnings.addAll(event.diagnostics());
                                candidateWarnings.addAll(triggerStep.warnings());
                                candidateWarnings.addAll(downstreamStep.warnings());
                                candidateWarnings.addAll(selectedStep.warnings());
                                candidateWarnings.addAll(downstreamFootprint.warnings());
                                candidateWarnings.addAll(selectedFootprint.warnings());
                                String originIdentity = String.join("\u0000",
                                        String.valueOf(event.emissionSite().deterministicId()),
                                        String.valueOf(event.eventHandlingClassFqn()),
                                        String.valueOf(event.eventHandlingMethodName()),
                                        String.valueOf(event.downstreamSagaFqn()));
                                if (addCandidate(candidates, adjacency, triggerSaga.sagaFqn(), triggerStep,
                                        downstreamFootprint, false, selectedSaga.sagaFqn(), selectedStep,
                                        selectedFootprint, true, match, ConflictOrigin.EVENT_CONSEQUENCE,
                                        downstreamSaga.sagaFqn(), originIdentity, candidateWarnings)) {
                                    eventCandidates++;
                                }
                            }
                        }
                    }
                }
            }
        }

        counts.put("selectionRecoveryConflictEdgesEmitted", recoveryCandidates);
        counts.put("selectionEventConflictEdgesEmitted", eventCandidates);
        counts.put("selectionConflictEdgesEmitted", candidates.size());
        return result(adjacency, candidates, counts, warnings);
    }

    private static int addRecoveryCandidates(LinkedHashMap<String, ConflictCandidate> candidates,
                                             LinkedHashMap<String, LinkedHashSet<String>> adjacency,
                                             LinkedHashSet<String> warnings,
                                             SagaDefinition leftSaga,
                                             StepDefinition leftStep,
                                             SagaDefinition rightSaga,
                                             StepDefinition rightStep,
                                             ScenarioGeneratorConfig config) {
        int emitted = 0;
        for (AccessSurface left : accessSurfaces(leftStep)) {
            for (AccessSurface right : accessSurfaces(rightStep)) {
                if (!left.recovery() && !right.recovery()) {
                    continue;
                }
                MatchResult match = match(left.footprint(), right.footprint(), config.allowTypeOnlyFallback());
                if (!match.matched() || readRead(left.footprint(), right.footprint())) {
                    continue;
                }
                List<String> candidateWarnings = new ArrayList<>(match.warnings());
                candidateWarnings.add("recovery-mediated selection edge");
                candidateWarnings.addAll(leftStep.analysisDiagnostics());
                candidateWarnings.addAll(rightStep.analysisDiagnostics());
                candidateWarnings.addAll(leftStep.warnings());
                candidateWarnings.addAll(rightStep.warnings());
                candidateWarnings.addAll(left.footprint().warnings());
                candidateWarnings.addAll(right.footprint().warnings());
                if (addCandidate(candidates, adjacency, leftSaga.sagaFqn(), leftStep, left.footprint(), true,
                        rightSaga.sagaFqn(), rightStep, right.footprint(), true, match,
                        ConflictOrigin.RECOVERY, null,
                        (left.recovery() ? "recovery" : "forward") + ":" + (right.recovery() ? "recovery" : "forward"),
                        candidateWarnings)) {
                    emitted++;
                }
            }
        }
        return emitted;
    }

    private static boolean addCandidate(LinkedHashMap<String, ConflictCandidate> candidates,
                                        LinkedHashMap<String, LinkedHashSet<String>> adjacency,
                                        String firstSagaFqn,
                                        StepDefinition firstStep,
                                        StepFootprint firstFootprint,
                                        boolean firstInputBound,
                                        String secondSagaFqn,
                                        StepDefinition secondStep,
                                        StepFootprint secondFootprint,
                                        boolean secondInputBound,
                                        MatchResult match,
                                        ConflictOrigin origin,
                                        String excludedSagaFqn,
                                        String originIdentity,
                                        List<String> candidateWarnings) {
        boolean alreadyOrdered = Comparator.nullsFirst(String::compareTo).compare(firstSagaFqn, secondSagaFqn) <= 0;
        String leftSagaFqn = alreadyOrdered ? firstSagaFqn : secondSagaFqn;
        String rightSagaFqn = alreadyOrdered ? secondSagaFqn : firstSagaFqn;
        StepDefinition leftStep = alreadyOrdered ? firstStep : secondStep;
        StepDefinition rightStep = alreadyOrdered ? secondStep : firstStep;
        StepFootprint leftFootprint = alreadyOrdered ? firstFootprint : secondFootprint;
        StepFootprint rightFootprint = alreadyOrdered ? secondFootprint : firstFootprint;
        boolean leftInputBound = alreadyOrdered ? firstInputBound : secondInputBound;
        boolean rightInputBound = alreadyOrdered ? secondInputBound : firstInputBound;
        String leftStepId = ScenarioIdGenerator.stepDefinitionId(leftSagaFqn, leftStep);
        String rightStepId = ScenarioIdGenerator.stepDefinitionId(rightSagaFqn, rightStep);
        ConflictKind kind = match.kind(leftFootprint.accessMode(), rightFootprint.accessMode());
        String id = ScenarioIdGenerator.selectionConflictEvidenceId(leftStepId, rightStepId,
                leftFootprint.aggregateKey(), rightFootprint.aggregateKey(), leftFootprint.accessMode(),
                rightFootprint.accessMode(), kind, origin.name(), originIdentity);
        ConflictCandidate candidate = new ConflictCandidate(id, leftSagaFqn, rightSagaFqn,
                leftStep, rightStep, leftFootprint, rightFootprint, leftStepId, rightStepId,
                kind, match.fallbackUsed(), origin, leftInputBound, rightInputBound,
                excludedSagaFqn, List.copyOf(candidateWarnings));
        if (candidates.putIfAbsent(id, candidate) != null) {
            return false;
        }
        adjacency.computeIfAbsent(leftSagaFqn, ignored -> new LinkedHashSet<>()).add(rightSagaFqn);
        adjacency.computeIfAbsent(rightSagaFqn, ignored -> new LinkedHashSet<>()).add(leftSagaFqn);
        return true;
    }

    private static List<AccessSurface> accessSurfaces(StepDefinition step) {
        List<AccessSurface> result = new ArrayList<>();
        step.footprints().forEach(footprint -> result.add(new AccessSurface(footprint, false)));
        step.compensationFootprints().forEach(footprint -> result.add(new AccessSurface(footprint, true)));
        return List.copyOf(result);
    }

    private static List<StepFootprint> allAccessFootprints(StepDefinition step) {
        return accessSurfaces(step).stream().map(AccessSurface::footprint).toList();
    }

    private static boolean readRead(StepFootprint left, StepFootprint right) {
        return left.accessMode() == AccessMode.READ && right.accessMode() == AccessMode.READ;
    }

    private static StepDefinition findTriggerStep(SagaDefinition saga, String triggerStepKey) {
        if (saga == null) return null;
        return sortedSteps(saga).stream()
                .filter(step -> Objects.equals(step.stepKey(), triggerStepKey)
                        || Objects.equals(ScenarioIdGenerator.stepDefinitionId(saga.sagaFqn(), step), triggerStepKey)
                        || Objects.equals(triggerStepKey, saga.sagaFqn() + "::" + step.name()))
                .findFirst().orElse(null);
    }

    private static LinkedHashMap<String, LinkedHashSet<String>> mutableAdjacency(Map<String, Set<String>> source) {
        LinkedHashMap<String, LinkedHashSet<String>> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, new LinkedHashSet<>(value)));
        return result;
    }

    private static Result result(LinkedHashMap<String, LinkedHashSet<String>> adjacency,
                                 LinkedHashMap<String, ConflictCandidate> candidates,
                                 LinkedHashMap<String, Integer> counts,
                                 LinkedHashSet<String> warnings) {
        LinkedHashMap<String, Set<String>> immutableAdjacency = new LinkedHashMap<>();
        adjacency.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> immutableAdjacency.put(entry.getKey(), Set.copyOf(entry.getValue())));
        List<ConflictCandidate> orderedCandidates = candidates.values().stream()
                .sorted(Comparator.comparing(ConflictCandidate::leftSagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::rightSagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::leftStepId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::rightStepId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(ConflictCandidate::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
        return new Result(Collections.unmodifiableMap(immutableAdjacency), List.copyOf(orderedCandidates),
                Collections.unmodifiableMap(counts), List.copyOf(warnings));
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
        boolean sameKeyText = leftExactKey != null && rightExactKey != null
                && Objects.equals(ExactKeyValueNormalizer.normalize(leftExactKey),
                ExactKeyValueNormalizer.normalize(rightExactKey));

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

        boolean leftResolved = leftExactKey != null
                && leftKey.confidence() != FootprintConfidence.TYPE_ONLY
                && leftKey.confidence() != FootprintConfidence.UNKNOWN;
        boolean rightResolved = rightExactKey != null
                && rightKey.confidence() != FootprintConfidence.TYPE_ONLY
                && rightKey.confidence() != FootprintConfidence.UNKNOWN;
        if (leftResolved && rightResolved) {
            return MatchResult.symbolic("symbolic aggregate candidate used for aggregate " + aggregateLabel(leftKey));
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
            ConflictOrigin origin,
            boolean leftInputBound,
            boolean rightInputBound,
            String excludedSagaFqn,
            List<String> warnings) {
    }

    public enum ConflictOrigin {
        FORWARD,
        RECOVERY,
        EVENT_CONSEQUENCE
    }

    private record AccessSurface(StepFootprint footprint, boolean recovery) {
    }

    public record Result(
            Map<String, Set<String>> adjacency,
            List<ConflictCandidate> conflictCandidates,
            Map<String, Integer> counts,
            List<String> warnings) {
    }
}
