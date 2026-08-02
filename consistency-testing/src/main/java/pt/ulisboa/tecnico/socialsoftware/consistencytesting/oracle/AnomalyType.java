package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/**
 * The isolation anomalies the {@link AnomalyAnalyzer} detects.
 * Every {@link Anomaly} carries one of these, so results can be grouped,
 * counted and text-searched.
 */
public enum AnomalyType {
    DIRTY_READ,
    NON_REPEATABLE_READ,
    WRITE_SKEW
}
