package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

/** Collects raw run outcome evidence for one campaign. */
final class CampaignMetrics {

    private final long startedAtEpochMillis;
    private int runsCompleted;
    private int anomaliesObserved;
    private int runsWithAnomalies;
    private @Nullable Long firstAnomalyElapsedMillis;
    private int interInvariantViolationsObserved;
    private int runsWithInterInvariantViolations;
    private @Nullable Long firstInterInvariantViolationElapsedMillis;
    // TreeSet keeps invariant names distinct and alphabetically ordered
    private final Set<String> violatedInterInvariantNames = new TreeSet<>();
    private int stepExceptionsObserved;
    private int runsWithStepExceptions;
    // TreeMap keeps status counts in alphabetical order
    private final Map<String, Integer> statusRunCounts = new TreeMap<>();

    CampaignMetrics(long startedAtEpochMillis) {
        this.startedAtEpochMillis = startedAtEpochMillis;
    }

    /**
     * Records a result using the current wall-clock time as its completion time.
     */
    void record(TestResult result) {
        record(result, System.currentTimeMillis());
    }

    /**
     * Records a result and measures time since campaign start. Negative elapsed
     * times are treated as zero.
     *
     * @param result                 the completed oracle result
     * @param completedAtEpochMillis completion time used for elapsed-time metrics
     */
    void record(TestResult result, long completedAtEpochMillis) {
        runsCompleted++;
        long elapsedMillis = Math.max(0, completedAtEpochMillis - startedAtEpochMillis);

        int anomalyCount = result.anomalies().size();
        anomaliesObserved += anomalyCount;
        if (anomalyCount > 0) {
            runsWithAnomalies++;
            firstAnomalyElapsedMillis = firstObservedAt(firstAnomalyElapsedMillis, elapsedMillis);
        }

        int interInvariantViolationCount = result.interInvariantViolations().values().stream()
                .mapToInt(violations -> violations.size())
                .sum();
        interInvariantViolationsObserved += interInvariantViolationCount;
        if (interInvariantViolationCount > 0) {
            runsWithInterInvariantViolations++;
            firstInterInvariantViolationElapsedMillis = firstObservedAt(
                    firstInterInvariantViolationElapsedMillis, elapsedMillis);
            violatedInterInvariantNames.addAll(result.interInvariantViolations().keySet());
        }

        int exceptionCount = result.exceptions().size();
        stepExceptionsObserved += exceptionCount;
        if (exceptionCount > 0) {
            runsWithStepExceptions++;
        }

        result.statuses().forEach(status -> statusRunCounts.merge(status.name(), 1, Integer::sum));
    }

    /**
     * Creates a snapshot using the planned-run count known by the campaign
     * progress tracker at this checkpoint. That count can grow as catalogs are
     * discovered, while these metrics only know completed runs.
     */
    OrchestrationReport.OutcomeMetrics snapshot(int runsPlanned) {
        return new OrchestrationReport.OutcomeMetrics(
                runsPlanned,
                runsCompleted,
                anomaliesObserved,
                runsWithAnomalies,
                firstAnomalyElapsedMillis,
                interInvariantViolationsObserved,
                runsWithInterInvariantViolations,
                firstInterInvariantViolationElapsedMillis,
                violatedInterInvariantNames.stream().toList(),
                stepExceptionsObserved,
                runsWithStepExceptions,
                statusRunCounts);
    }

    private static Long firstObservedAt(@Nullable Long previous, long elapsedMillis) {
        return previous == null ? elapsedMillis : Math.min(previous, elapsedMillis);
    }
}
