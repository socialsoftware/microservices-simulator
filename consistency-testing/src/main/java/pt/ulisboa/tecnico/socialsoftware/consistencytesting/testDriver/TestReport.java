package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.AnomalyType;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

/** A serialization-friendly, flattened view of a {@link TestResult}. */
public record TestReport(
        List<String> schedule,
        List<String> statuses,
        List<AnomalyView> anomalies,
        Map<String, List<InterInvariantViolationView>> interInvariantViolations,
        List<EffectView> effectSequence,
        List<ReadsFromView> readsFromRelations,
        Map<String, String> stepExceptions,
        int functionalityCount) {

    public record InterInvariantViolationView(String description) {
    }

    public record ReadsFromView(String reader, String writer, String aggregateType) {
    }

    /**
     * {@code type} is the {@link AnomalyType} name verbatim
     * (e.g. {@code "DIRTY_READ"}) so reports are text-searchable by anomaly kind,
     * independently of the free-form description.
     */
    public record AnomalyView(String type, String description) {
    }

    public record EffectView(
            int seq, String step, String stepKind, String effectKind, String aggregateType, Integer aggregateId) {
    }

    public static TestReport from(TestResult result) {
        List<String> schedule = result.schedule().stream()
                .map(Object::toString)
                .toList();

        List<String> statuses = result.statuses().stream()
                .map(Enum::name)
                .sorted()
                .toList();

        Map<String, List<InterInvariantViolationView>> interInvariantViolations = result.interInvariantViolations()
                .entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().stream()
                                .map(TestReport::toView)
                                .toList()));

        List<ReadsFromView> readsFrom = result.readsFromRelations().stream()
                .map(TestReport::toView)
                .sorted(Comparator.comparing(ReadsFromView::reader)
                        .thenComparing(ReadsFromView::aggregateType)
                        .thenComparing(ReadsFromView::writer))
                .toList();

        Map<String, String> stepExceptions = result.exceptions().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> describeException(entry.getValue())));

        List<AnomalyView> anomalies = result.anomalies().stream()
                .map(TestReport::toView)
                .toList();

        List<EffectView> effectSequence = result.effectSequence().stream()
                .map(TestReport::toView)
                .toList();

        int functionalityCount = result.functionalities().size();

        return new TestReport(
                schedule, statuses, anomalies, interInvariantViolations, effectSequence, readsFrom,
                stepExceptions, functionalityCount);
    }

    private static AnomalyView toView(Anomaly anomaly) {
        return new AnomalyView(anomaly.type().name(), anomaly.description());
    }

    private static EffectView toView(StepEffect stepEffect) {
        return new EffectView(
                stepEffect.sequenceNumber(),
                stepEffect.stepId().toString(),
                stepEffect.stepKind().name(),
                stepEffect.effectKind().name(),
                stepEffect.aggregateType(),
                stepEffect.aggregateId());
    }

    private static ReadsFromView toView(ReadsFromRelation relation) {
        return new ReadsFromView(
                relation.reader().toString(),
                relation.writer().toString(),
                relation.aggregateType());
    }

    private static InterInvariantViolationView toView(InterInvariantViolation interInvariantViolation) {
        return new InterInvariantViolationView(interInvariantViolation.description());
    }

    private static String describeException(Exception exception) {
        String message = exception.getMessage();
        return message == null
                ? exception.getClass().getSimpleName()
                : exception.getClass().getSimpleName() + ": " + message;
    }
}
