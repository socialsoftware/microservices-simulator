package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantsProvider;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.FunctionalityCatalogsProvider;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;

class QuizzesConsistencyCatalogValidation {

    @Test
    void bootsProvidersAndProfilesEveryFunctionalityAlone() {
        TestDriver driver = new TestDriver(
                QuizzesSimulator.class,
                List.of(),
                Path.of("target", "consistency-validation-reports"));
        driver.init();
        try {
            assertNotNull(driver.getApplicationBean(InterInvariantsProvider.class));
            List<FunctionalityCatalog> catalogs = driver
                    .getApplicationBean(FunctionalityCatalogsProvider.class)
                    .getCatalogs();

            assertFalse(catalogs.isEmpty(), "at least one catalog is required");
            assertEquals(catalogs.size(), new HashSet<>(catalogs.stream()
                    .map(FunctionalityCatalog::name).toList()).size(),
                    "catalog names must be unique");

            for (FunctionalityCatalog catalog : catalogs) {
                assertFalse(catalog.funcFactories().isEmpty(),
                        () -> "catalog has no functionalities: " + catalog.name());
                List<String> failures = new ArrayList<>();
                for (var entry : catalog.funcFactories().entrySet()) {
                    FunctionalityCatalog singleton = new FunctionalityCatalog(
                            catalog.name() + "-" + entry.getKey(),
                            catalog.initialStateSetup(),
                            Map.of(entry.getKey(), entry.getValue()));
                    try {
                        assertEquals(1, driver.profileFunctionalities(singleton).size());
                    } catch (RuntimeException e) {
                        failures.add(entry.getKey() + ": " + e.getMessage());
                    }
                }
                assertTrue(failures.isEmpty(),
                        () -> "solo profiling failures in " + catalog.name() + ":\n" + String.join("\n", failures));
            }
        } finally {
            driver.shutdown();
        }
    }
}
