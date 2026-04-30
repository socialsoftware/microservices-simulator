package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class ScenarioIdGenerator {

    private static final HexFormat HEX = HexFormat.of();
    private static final Comparator<String> STRING_ORDER = Comparator.nullsFirst(String::compareTo);

    private ScenarioIdGenerator() {
    }

    public static String inputVariantId(String sagaFqn,
                                        String sourceClassFqn,
                                        String sourceMethodName,
                                        String sourceBindingName,
                                        InputResolutionStatus resolutionStatus,
                                        String stableSourceText,
                                        String provenanceText,
                                        List<String> constructorArgumentSummaries,
                                        Map<String, String> logicalKeyBindings) {
        return hash(digest -> {
            updateString(digest, "input-variant");
            updateString(digest, normalize(sagaFqn));
            updateString(digest, normalize(sourceClassFqn));
            updateString(digest, normalize(sourceMethodName));
            updateString(digest, normalize(sourceBindingName));
            updateString(digest, resolutionStatus == null ? null : resolutionStatus.name());
            updateString(digest, normalize(stableSourceText));
            updateString(digest, normalize(provenanceText));
            updateStrings(digest, sortedStrings(constructorArgumentSummaries));
            updateMap(digest, logicalKeyBindings);
        });
    }

    public static String sagaInstanceId(String sagaFqn, String inputVariantId) {
        return hash(digest -> {
            updateString(digest, "saga-instance");
            updateString(digest, normalize(sagaFqn));
            updateString(digest, normalize(inputVariantId));
        });
    }

    public static String stepDefinitionId(String sagaFqn, StepDefinition step) {
        String provided = normalize(step == null ? null : step.deterministicId());
        if (provided != null) {
            return provided;
        }

        return hash(digest -> {
            updateString(digest, "step-definition");
            updateString(digest, normalize(sagaFqn));
            updateString(digest, normalize(step == null ? null : step.stepKey()));
            updateString(digest, normalize(step == null ? null : step.name()));
            updateInt(digest, step == null ? 0 : step.orderIndex());
            updateStrings(digest, sortedStrings(step == null ? List.of() : step.predecessorStepKeys()));
            updateFootprints(digest, step == null ? List.of() : step.footprints());
        });
    }

    public static String scheduledStepId(String sagaInstanceId, String stepId, int scheduleOrder) {
        return hash(digest -> {
            updateString(digest, "scheduled-step");
            updateString(digest, normalize(sagaInstanceId));
            updateString(digest, normalize(stepId));
            updateInt(digest, scheduleOrder);
        });
    }

    public static String conflictEvidenceId(String leftScheduledStepId,
                                            String rightScheduledStepId,
                                            AggregateKey leftAggregateKey,
                                            AggregateKey rightAggregateKey,
                                            AccessMode leftAccessMode,
                                            AccessMode rightAccessMode,
                                            ConflictKind kind) {
        return hash(digest -> {
            updateString(digest, "conflict-evidence");
            updateString(digest, normalize(leftScheduledStepId));
            updateString(digest, normalize(rightScheduledStepId));
            updateAggregateKey(digest, leftAggregateKey);
            updateAggregateKey(digest, rightAggregateKey);
            updateString(digest, leftAccessMode == null ? null : leftAccessMode.name());
            updateString(digest, rightAccessMode == null ? null : rightAccessMode.name());
            updateString(digest, kind == null ? null : kind.name());
        });
    }

    public static String scenarioPlanId(ScenarioKind kind,
                                        List<SagaInstance> sagaInstances,
                                        List<InputVariant> inputs,
                                        List<ScheduledStep> expandedSchedule,
                                        List<ConflictEvidence> conflictEvidence) {
        return hash(digest -> {
            updateString(digest, "scenario-plan");
            updateString(digest, ScenarioPlan.SCHEMA_VERSION);
            updateString(digest, kind == null ? null : kind.name());
            updateSagaInstances(digest, sagaInstances);
            updateInputVariants(digest, inputs);
            updateScheduledSteps(digest, expandedSchedule);
            updateConflictEvidence(digest, conflictEvidence);
        });
    }

    private static void updateSagaInstances(MessageDigest digest, List<SagaInstance> sagaInstances) {
        List<SagaInstance> sorted = sagaInstances == null ? List.of() : sagaInstances.stream()
                .sorted(Comparator
                        .comparing(SagaInstance::sagaFqn, STRING_ORDER)
                        .thenComparing(SagaInstance::inputVariantId, STRING_ORDER)
                        .thenComparing(SagaInstance::deterministicId, STRING_ORDER))
                .toList();

        updateInt(digest, sorted.size());
        for (SagaInstance sagaInstance : sorted) {
            updateString(digest, sagaInstance.sagaFqn());
            updateString(digest, sagaInstance.inputVariantId());
            updateString(digest, sagaInstance.deterministicId());
        }
    }

    private static void updateInputVariants(MessageDigest digest, List<InputVariant> inputs) {
        List<InputVariant> sorted = inputs == null ? List.of() : inputs.stream()
                .sorted(Comparator
                        .comparing(InputVariant::sagaFqn, STRING_ORDER)
                        .thenComparing(InputVariant::deterministicId, STRING_ORDER)
                        .thenComparing(InputVariant::sourceClassFqn, STRING_ORDER)
                        .thenComparing(InputVariant::sourceMethodName, STRING_ORDER)
                        .thenComparing(InputVariant::sourceBindingName, STRING_ORDER))
                .toList();

        updateInt(digest, sorted.size());
        for (InputVariant input : sorted) {
            updateString(digest, input.sagaFqn());
            updateString(digest, input.deterministicId());
        }
    }

    private static void updateScheduledSteps(MessageDigest digest, List<ScheduledStep> steps) {
        List<ScheduledStep> ordered = steps == null ? List.of() : steps;
        updateInt(digest, ordered.size());
        for (ScheduledStep step : ordered) {
            updateString(digest, step == null ? null : step.deterministicId());
        }
    }

    private static void updateConflictEvidence(MessageDigest digest, List<ConflictEvidence> conflictEvidence) {
        List<ConflictEvidence> sorted = conflictEvidence == null ? List.of() : conflictEvidence.stream()
                .sorted(Comparator
                        .comparing(ConflictEvidence::deterministicId, STRING_ORDER)
                        .thenComparing(ConflictEvidence::leftScheduledStepId, STRING_ORDER)
                        .thenComparing(ConflictEvidence::rightScheduledStepId, STRING_ORDER))
                .toList();

        updateInt(digest, sorted.size());
        for (ConflictEvidence evidence : sorted) {
            updateString(digest, evidence.deterministicId());
            updateString(digest, evidence.leftScheduledStepId());
            updateString(digest, evidence.rightScheduledStepId());
            updateAggregateKey(digest, evidence.leftAggregateKey());
            updateAggregateKey(digest, evidence.rightAggregateKey());
            updateString(digest, evidence.leftAccessMode() == null ? null : evidence.leftAccessMode().name());
            updateString(digest, evidence.rightAccessMode() == null ? null : evidence.rightAccessMode().name());
            updateString(digest, evidence.kind() == null ? null : evidence.kind().name());
        }
    }

    private static void updateStrings(MessageDigest digest, List<String> values) {
        List<String> ordered = values == null ? List.of() : values;
        updateInt(digest, ordered.size());
        for (String value : ordered) {
            updateString(digest, value);
        }
    }

    private static void updateFootprints(MessageDigest digest, List<StepFootprint> footprints) {
        List<StepFootprint> ordered = footprints == null ? List.of() : footprints.stream()
                .sorted(Comparator
                        .comparing((StepFootprint footprint) -> aggregateKeySignature(footprint == null ? null : footprint.aggregateKey()), STRING_ORDER)
                        .thenComparing(footprint -> footprint == null || footprint.accessMode() == null ? null : footprint.accessMode().name(), STRING_ORDER))
                .toList();

        updateInt(digest, ordered.size());
        for (StepFootprint footprint : ordered) {
            updateAggregateKey(digest, footprint == null ? null : footprint.aggregateKey());
            updateString(digest, footprint == null || footprint.accessMode() == null ? null : footprint.accessMode().name());
        }
    }

    private static String aggregateKeySignature(AggregateKey aggregateKey) {
        if (aggregateKey == null) {
            return "";
        }
        return safeSegment(aggregateKey.aggregateTypeName())
                + "|"
                + safeSegment(aggregateKey.aggregateName())
                + "|"
                + safeSegment(aggregateKey.keyText())
                + "|"
                + (aggregateKey.confidence() == null ? FootprintConfidence.UNKNOWN.name() : aggregateKey.confidence().name());
    }

    private static void updateAggregateKey(MessageDigest digest, AggregateKey aggregateKey) {
        updateString(digest, aggregateKey == null ? null : aggregateKey.aggregateTypeName());
        updateString(digest, aggregateKey == null ? null : aggregateKey.aggregateName());
        updateString(digest, aggregateKey == null ? null : aggregateKey.keyText());
        updateString(digest, aggregateKey == null || aggregateKey.confidence() == null ? null : aggregateKey.confidence().name());
    }

    private static void updateMap(MessageDigest digest, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            updateInt(digest, 0);
            return;
        }

        List<Map.Entry<String, String>> entries = values.entrySet().stream()
                .map(entry -> Map.entry(normalize(entry.getKey()), normalize(entry.getValue())))
                .filter(entry -> entry.getKey() != null)
                .sorted(Map.Entry.comparingByKey(STRING_ORDER))
                .toList();

        updateInt(digest, entries.size());
        for (Map.Entry<String, String> entry : entries) {
            updateString(digest, entry.getKey());
            updateString(digest, entry.getValue());
        }
    }

    private static List<String> sortedStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(ScenarioIdGenerator::normalize)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private static String hash(Consumer<MessageDigest> updateAction) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateAction.accept(digest);
            return HEX.formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing SHA-256 support", exception);
        }
    }

    private static void updateString(MessageDigest digest, String value) {
        if (value == null) {
            updateInt(digest, -1);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeSegment(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }
}
