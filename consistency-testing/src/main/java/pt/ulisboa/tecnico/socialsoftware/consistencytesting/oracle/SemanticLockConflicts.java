package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA;

/** Shared classification for semantic-lock conflicts reported by the simulator. */
final class SemanticLockConflicts {

    private SemanticLockConflicts() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static boolean isConflict(Exception exception) {
        return exception instanceof SimulatorException simulatorException
                && AGGREGATE_BEING_USED_IN_OTHER_SAGA.equals(simulatorException.getErrorMessage());
    }
}
