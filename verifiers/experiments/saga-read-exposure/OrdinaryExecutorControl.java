package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;

import java.time.LocalDateTime;

/** Experiment-only clock fixture around the unchanged ordinary executor CLI. */
public final class OrdinaryExecutorControl {
    public static void main(String[] args) throws Exception {
        LocalDateTime fixed = LocalDateTime.of(2030, 1, 1, 12, 0);
        try (MockedStatic<DateHandler> clock = Mockito.mockStatic(DateHandler.class, Mockito.CALLS_REAL_METHODS)) {
            clock.when(DateHandler::now).thenReturn(fixed);
            System.out.println("Experiment clock fixture: DateHandler.now()=" + fixed);
            ScenarioExecutorCli.main(args);
        }
    }
}
