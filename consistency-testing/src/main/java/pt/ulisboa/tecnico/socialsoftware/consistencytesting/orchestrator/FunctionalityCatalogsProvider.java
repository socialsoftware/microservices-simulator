package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;

/**
 * The scenarios an application exposes to be explored, each one holds an
 * initial state plus the functionalities that run on top of it.
 * <p>
 * Applications implement this as a Spring bean in their own test sources:
 *
 * <pre>{@code
 * &#64;Component
 * &#64;Profile("oracle")
 * public class MyAppFunctionalityCatalogsProvider implements FunctionalityCatalogsProvider { ... }
 * }</pre>
 *
 * The {@code oracle} profile is active only while the tool runs the
 * application, so this never ships in the application's production artifact.
 */
public interface FunctionalityCatalogsProvider {

    /**
     * The catalog(s) to explore.
     * <p>
     * Multiple catalogs are useful when it makes sense to test different initial
     * states (the tool never pairs functionalities across catalogs, since they
     * never share an initial state).
     */
    List<FunctionalityCatalog> getCatalogs();
}
