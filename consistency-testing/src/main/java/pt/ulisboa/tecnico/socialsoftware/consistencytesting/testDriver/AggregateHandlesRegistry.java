package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps stable logical handles (e.g. {@code "tournamentA"}) to the run-local
 * aggregate IDs produced during initial-state setup.
 * <p>
 * Because the database is wiped between test runs, aggregate IDs are reassigned
 * on every run and cannot be used for cross-run identification. The logical
 * handle serves as a stable, cross-run identity. The setup phase registers
 * created aggregates under a handle, and factories resolve these handles back
 * to the active run's IDs.
 * <p>
 * <h2>Example Scenario:</h2>
 * <ol>
 * <li>The setup phase creates a tournament in a specific cross-run stable
 * state and registers it under the handle {@code "tournamentA"}.</li>
 * <li>A factory instantiates the {@code "addParticipantToTournamentA"}
 * functionality.</li>
 * <li>To target the correct tournament, this functionality must receive the
 * active run's resolved aggregate ID for {@code "tournamentA"} as its
 * {@code tournamentId} argument:
 * <ul>
 * <li><b>Run 1:</b> {@code "tournamentA"} resolves to {@code 1} &rarr;
 * Functionality receives {@code tournamentId = 1}</li>
 * <li><b>Run 2:</b> {@code "tournamentA"} resolves to {@code 5} &rarr;
 * Functionality receives {@code tournamentId = 5}</li>
 * </ul>
 * </li>
 * </ol>
 */
public final class AggregateHandlesRegistry {

    private final Map<String, Integer> idsByHandle = new LinkedHashMap<>();
    private final Map<Integer, String> handlesById = new LinkedHashMap<>();

    /**
     * Registers {@code aggregateId} under {@code handle}. Both must be new:
     * a handle identifies exactly one aggregate and vice versa.
     */
    public AggregateHandlesRegistry register(String handle, Integer aggregateId) {
        if (idsByHandle.containsKey(handle)) {
            throw new IllegalArgumentException(
                    "Handle '%s' is already registered for aggregate id %d"
                            .formatted(handle, idsByHandle.get(handle)));
        }
        if (handlesById.containsKey(aggregateId)) {
            throw new IllegalArgumentException(
                    "Aggregate id %d is already registered under handle '%s'"
                            .formatted(aggregateId, handlesById.get(aggregateId)));
        }

        idsByHandle.put(handle, aggregateId);
        handlesById.put(aggregateId, handle);
        return this;
    }

    /** The current run's aggregate id registered under {@code handle}. */
    public Integer idOf(String handle) {
        Integer aggregateId = idsByHandle.get(handle);
        if (aggregateId == null) {
            throw new IllegalArgumentException(
                    "No aggregate registered under handle '%s'; known handles: %s"
                            .formatted(handle, idsByHandle.keySet()));
        }
        return aggregateId;
    }

    /**
     * The handle {@code aggregateId} was registered under, or empty for
     * aggregates the initial-state setup did not create (e.g. aggregates a
     * functionality created mid-run).
     */
    public Optional<String> handleOf(Integer aggregateId) {
        return Optional.ofNullable(handlesById.get(aggregateId));
    }
}
