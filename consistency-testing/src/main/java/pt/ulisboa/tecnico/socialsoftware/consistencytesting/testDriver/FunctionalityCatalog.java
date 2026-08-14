package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

/**
 * A pool of functionalities the planner works over, bound to the shared
 * initial state they all run on top of.
 * <p>
 * {@code initialStateSetup} is invoked once per oracle run (the database is
 * wiped after every run): it creates the initial aggregates and registers each
 * one in the {@link AggregateHandlesRegistry} it returns. Each factory then
 * builds a FRESH {@link WorkflowFunctionality} instance for that run, resolving
 * the handles it needs (e.g., for arguments) through the registry — which is
 * what keeps a functionality logically stable ("the same") across runs even
 * though every concrete aggregate id possibly changed.
 *
 * @param initialStateSetup creates the initial database state and returns the
 *                          registry of the aggregates it created
 * @param funcFactories     one factory per functionality, keyed by the stable
 *                          {@link FunctionalityId} the planner and its outputs
 *                          refer to it by
 */
public record FunctionalityCatalog(
        Supplier<AggregateHandlesRegistry> initialStateSetup,
        Map<FunctionalityId, Function<AggregateHandlesRegistry, WorkflowFunctionality>> funcFactories) {

    public FunctionalityCatalog {
        funcFactories = Map.copyOf(funcFactories);
    }
}
