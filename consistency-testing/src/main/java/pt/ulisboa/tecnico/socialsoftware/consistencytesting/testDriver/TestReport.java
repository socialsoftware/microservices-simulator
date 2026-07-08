package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

/** A serialization-friendly, flattened view of a {@link TestResult}. */
public record TestReport(
        List<String> schedule,
        List<String> statuses,
        Map<String, List<InterInvariantViolationView>> interInvariantViolations,
        List<ReadsFromView> readsFromRelations,
        Map<String, String> stepExceptions,
        int functionalityCount) {

    public record InterInvariantViolationView(String description) {
    }

    public record ReadsFromView(String reader, String writer, String aggregateType) {
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

        int functionalityCount = result.functionalities().size();

        return new TestReport(
                schedule, statuses, interInvariantViolations, readsFrom, stepExceptions, functionalityCount);
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
