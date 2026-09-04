package pt.ulisboa.tecnico.socialsoftware.quizzesfull.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantsProvider;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.FunctionalityCatalogsProvider;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityCatalog;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSimulator;

class QuizzesFullConsistencyCatalogValidation {

    @Test
    void bootsProvidersAndProfilesEveryFunctionalityAlone() {
        TestDriver driver = new TestDriver(
                QuizzesFullSimulator.class,
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
                assertEquals(catalog.funcFactories().size(),
                        driver.profileFunctionalities(catalog).size(),
                        () -> "not every functionality profiled: " + catalog.name());
            }
        } finally {
            driver.shutdown();
        }
    }
}
