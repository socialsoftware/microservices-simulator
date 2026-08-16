package pt.ulisboa.tecnico.socialsoftware.quizzes.executor;

import org.springframework.jdbc.core.JdbcTemplate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import javax.sql.DataSource;
import java.util.List;

/** Reads the latest raw persisted Tournament Saga state and decodes it through the production converter. */
public final class QuizzesPersistedSagaStateObserver {
    public static final String SOURCE = "RAW_DATABASE_COLUMN";
    public static final String TABLE = "saga_tournament";
    public static final String COLUMN = "saga_state";
    public static final String SELECTION = "LATEST_AGGREGATE_VERSION";
    public static final String DECODER = SagaStateConverter.class.getName();
    public static final String VALUE = "VALUE";
    public static final String SQL_NULL = "SQL_NULL";
    public static final String DECODED = "DECODED";
    public static final String NOT_DECODED = "NOT_DECODED";
    public static final String DECODE_FAILED = "DECODE_FAILED";

    private static final String TOURNAMENT_QUERY = """
            select aggregate_id, version, saga_state
            from saga_tournament
            where aggregate_id = ?
              and version = (select max(version) from saga_tournament where aggregate_id = ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public QuizzesPersistedSagaStateObserver(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public PersistedSagaState observeTournament(int aggregateId) {
        if (aggregateId < 1) {
            throw new IllegalArgumentException("Tournament aggregate identity must be positive");
        }
        List<PersistedSagaState> rows = jdbcTemplate.query(
                TOURNAMENT_QUERY,
                (resultSet, rowNumber) -> decodePersistedValue(
                        resultSet.getInt("aggregate_id"),
                        resultSet.getObject("version", Long.class),
                        resultSet.getString("saga_state")),
                aggregateId,
                aggregateId);
        return requireExactTournamentState(aggregateId, rows);
    }

    public static PersistedSagaState decodePersistedValue(
            int aggregateId, Long aggregateVersion, String rawValue) {
        if (rawValue == null) {
            return new PersistedSagaState(
                    aggregateId, aggregateVersion, null, SQL_NULL, NOT_DECODED,
                    null, null, null, DECODER, SOURCE, TABLE, COLUMN, SELECTION);
        }

        try {
            SagaAggregate.SagaState decoded = new SagaStateConverter().convertToEntityAttribute(rawValue);
            if (!(decoded instanceof Enum<?> decodedEnum)) {
                return decodeFailed(aggregateId, aggregateVersion, rawValue,
                        "SagaStateConverter returned a non-enum SagaState");
            }
            return new PersistedSagaState(
                    aggregateId,
                    aggregateVersion,
                    rawValue,
                    VALUE,
                    DECODED,
                    decodedEnum.getDeclaringClass().getName(),
                    decodedEnum.name(),
                    null,
                    DECODER,
                    SOURCE,
                    TABLE,
                    COLUMN,
                    SELECTION);
        } catch (RuntimeException failure) {
            return decodeFailed(aggregateId, aggregateVersion, rawValue,
                    failure.getClass().getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage()));
        }
    }

    private static PersistedSagaState decodeFailed(
            int aggregateId, Long aggregateVersion, String rawValue, String failure) {
        return new PersistedSagaState(
                aggregateId, aggregateVersion, rawValue, VALUE, DECODE_FAILED,
                null, null, failure, DECODER, SOURCE, TABLE, COLUMN, SELECTION);
    }

    public static PersistedSagaState requireExactTournamentState(
            int expectedAggregateId, List<PersistedSagaState> rows) {
        if (rows == null || rows.size() != 1) {
            throw new IllegalStateException("Expected one latest persisted Tournament Saga-state row for aggregate "
                    + expectedAggregateId + " but found " + (rows == null ? 0 : rows.size()));
        }
        PersistedSagaState observed = rows.getFirst();
        boolean sqlNull = SQL_NULL.equals(observed.valueStatus())
                && observed.rawValue() == null
                && NOT_DECODED.equals(observed.decodeStatus())
                && observed.decodedStateClass() == null
                && observed.decodedStateName() == null
                && observed.decodeFailure() == null;
        boolean decoded = VALUE.equals(observed.valueStatus())
                && observed.rawValue() != null
                && !observed.rawValue().isBlank()
                && DECODED.equals(observed.decodeStatus())
                && observed.decodedStateClass() != null
                && !observed.decodedStateClass().isBlank()
                && observed.decodedStateName() != null
                && !observed.decodedStateName().isBlank()
                && observed.decodeFailure() == null;
        boolean decodeFailed = VALUE.equals(observed.valueStatus())
                && observed.rawValue() != null
                && !observed.rawValue().isBlank()
                && DECODE_FAILED.equals(observed.decodeStatus())
                && observed.decodedStateClass() == null
                && observed.decodedStateName() == null
                && observed.decodeFailure() != null
                && !observed.decodeFailure().isBlank();
        if (observed.aggregateId() != expectedAggregateId
                || observed.aggregateVersion() == null
                || observed.aggregateVersion() < 0
                || !(sqlNull || decoded || decodeFailed)
                || !DECODER.equals(observed.decoder())
                || !SOURCE.equals(observed.source())
                || !TABLE.equals(observed.storageTable())
                || !COLUMN.equals(observed.storageColumn())
                || !SELECTION.equals(observed.rowSelection())) {
            throw new IllegalStateException("Persisted Tournament Saga-state evidence is incomplete or mismatched for aggregate "
                    + expectedAggregateId + ": " + observed);
        }
        return observed;
    }

    public record PersistedSagaState(
            int aggregateId,
            Long aggregateVersion,
            String rawValue,
            String valueStatus,
            String decodeStatus,
            String decodedStateClass,
            String decodedStateName,
            String decodeFailure,
            String decoder,
            String source,
            String storageTable,
            String storageColumn,
            String rowSelection) {
    }
}
