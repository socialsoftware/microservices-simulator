package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Bounded, behavior-centered observations for measuring exploration breadth.
 * Unlike the detailed fingerprint, signals omit step names, occurrence counts,
 * concrete aggregate IDs, and event-capture ancestry. A signal is evidence,
 * not necessarily a correctness violation.
 */
public record BehavioralSignals(String schema, String hash, List<String> signals) {

    public static final String SCHEMA = "behavioral-signals-v2";

    public BehavioralSignals {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(hash);
        signals = List.copyOf(signals);
    }

    public static BehavioralSignals from(TestResult result) {
        Objects.requireNonNull(result);
        TreeSet<String> signals = new TreeSet<>();

        result.schedule().stream()
                .filter(step -> step.stepKind() != StepKind.FUNCTIONALITY)
                .forEach(step -> signals.add(signal("path", actor(step), step.stepKind().name())));

        result.effectSequence().forEach(effect -> signals.add(signal(
                "effect", actor(effect.stepId()), effect.stepKind().name(),
                effect.effectKind().name(), effect.aggregateType())));
        addInteractions(signals, result.effectSequence());

        result.readsFromRelations().forEach(relation -> signals.add(signal(
                "reads-from", actor(relation.writer()), actor(relation.reader()),
                relation.aggregateType())));
        addUncommittedReadExposures(signals, result);

        result.semanticLockTrace().forEach(activity -> signals.add(signal(
                "semantic-lock", actor(activity.stepId()),
                activity.semanticLock().toSelector(), activity.outcome().name())));

        result.statuses().forEach(status -> signals.add(signal("status", status.name())));
        result.anomalies().forEach(anomaly -> signals.add(anomalySignal(anomaly)));
        result.interInvariantViolations().keySet().forEach(
                invariant -> signals.add(signal("inter-invariant-violation", invariant)));
        result.exceptions().forEach((step, exception) -> signals.add(signal(
                "exception", actor(step), exception.getClass().getName())));

        List<String> canonicalSignals = List.copyOf(signals);
        return new BehavioralSignals(SCHEMA, sha256(canonicalSignals), canonicalSignals);
    }

    /** Records ordered accesses by distinct actors when they touch the same aggregate and at least one writes. */
    private static void addInteractions(TreeSet<String> signals, List<StepEffect> effects) {
        for (int firstIndex = 0; firstIndex < effects.size(); firstIndex++) {
            StepEffect first = effects.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < effects.size(); secondIndex++) {
                StepEffect second = effects.get(secondIndex);
                String firstActor = actor(first.stepId());
                String secondActor = actor(second.stepId());
                if (firstActor.equals(secondActor)
                        || !Objects.equals(first.aggregateId(), second.aggregateId())
                        || !first.aggregateType().equals(second.aggregateType())
                        || (!first.isWrite() && !second.isWrite())) {
                    continue;
                }
                signals.add(signal(
                        "interaction", firstActor, first.effectKind().name(),
                        "before", secondActor, second.effectKind().name(),
                        first.aggregateType()));
            }
        }
    }

    /**
     * Foreign-saga read before writer's terminal path. This is exposure evidence,
     * not by itself a dirty-read anomaly; later writer outcome stays visible.
     */
    private static void addUncommittedReadExposures(TreeSet<String> signals, TestResult result) {
        List<StepId> schedule = result.schedule();
        for (ReadsFromRelation relation : result.readsFromRelations()) {
            StepId writer = relation.writer();
            StepId reader = relation.reader();
            if (writer.equals(StepId.forInitialStateSetupStep())
                    || writer.stepKind() != StepKind.FUNCTIONALITY
                    || !isApplicationStep(reader)
                    || writer.getFunctionalityId().equals(reader.getFunctionalityId())) {
                continue;
            }

            int readerPosition = schedule.indexOf(reader);
            if (readerPosition < 0) {
                continue;
            }
            TerminalPath terminalPath = terminalPath(schedule, writer.getFunctionalityId());
            if (terminalPath != null && terminalPath.position() < readerPosition) {
                continue;
            }
            String writerOutcome = terminalPath == null ? "UNRESOLVED" : terminalPath.outcome();
            signals.add(signal(
                    "uncommitted-read-exposure", actor(writer), actor(reader),
                    relation.aggregateType(), writerOutcome));
        }
    }

    private static boolean isApplicationStep(StepId step) {
        return switch (step.stepKind()) {
            case FUNCTIONALITY, EVENT_HANDLER -> true;
            case COMPENSATION, COMMIT, ABORT -> false;
        };
    }

    /** Finds the first commit, compensation, or abort for this writer in the schedule. */
    private static TerminalPath terminalPath(List<StepId> schedule, FunctionalityId functionalityId) {
        for (int position = 0; position < schedule.size(); position++) {
            StepId step = schedule.get(position);
            if (!step.getFunctionalityId().equals(functionalityId)) {
                continue;
            }
            String outcome = switch (step.stepKind()) {
                case COMMIT -> "COMMITTED";
                case COMPENSATION, ABORT -> "COMPENSATED";
                case FUNCTIONALITY, EVENT_HANDLER -> null;
            };
            if (outcome != null) {
                return new TerminalPath(position, outcome);
            }
        }
        return null;
    }

    /** Schedule position and resulting terminal outcome for a writer. */
    private record TerminalPath(int position, String outcome) {
    }

    private static String anomalySignal(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.DirtyRead dirtyRead -> signal(
                    "anomaly", anomaly.type().name(), actor(dirtyRead.doomedWriter()),
                    actor(dirtyRead.reader()), dirtyRead.aggregateType());
            case Anomaly.NonRepeatableRead nonRepeatableRead -> signal(
                    "anomaly", anomaly.type().name(),
                    actor(nonRepeatableRead.firstWriter()), actor(nonRepeatableRead.firstRead()),
                    actor(nonRepeatableRead.secondWriter()), actor(nonRepeatableRead.secondRead()),
                    nonRepeatableRead.aggregateType());
            case Anomaly.WriteSkew writeSkew -> signal(
                    "anomaly", anomaly.type().name(),
                    writeSkew.functionalityA().behavioralActorIdentity(),
                    writeSkew.functionalityB().behavioralActorIdentity(),
                    writeSkew.aggregateTypeOverwrittenByB(),
                    writeSkew.aggregateTypeOverwrittenByA());
        };
    }

    private static String actor(StepId step) {
        return step.behavioralActorIdentity();
    }

    private static String signal(String... parts) {
        List<String> escaped = new ArrayList<>(parts.length);
        for (String part : parts) {
            escaped.add(part.replace("%", "%25")
                    .replace("|", "%7C")
                    .replace("\r", "%0D")
                    .replace("\n", "%0A"));
        }
        return String.join("|", escaped);
    }

    private static String sha256(List<String> signals) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(SCHEMA.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '\n');
            for (int index = 0; index < signals.size(); index++) {
                if (index > 0) {
                    digest.update((byte) '\n');
                }
                digest.update(signals.get(index).getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to hash behavioral signals", e);
        }
    }
}
