package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ScenarioCatalogReader {
    private final WholePackageReader wholePackageReader;
    private final SelectedPackageReader selectedPackageReader;

    public ScenarioCatalogReader() {
        this(new ScenarioCatalogPackageReader());
    }

    ScenarioCatalogReader(ScenarioCatalogPackageReader packageReader) {
        Objects.requireNonNull(packageReader);
        this.wholePackageReader = packageReader::read;
        this.selectedPackageReader = packageReader::readSelected;
    }

    ScenarioCatalogReader(WholePackageReader wholePackageReader,
                          SelectedPackageReader selectedPackageReader) {
        this.wholePackageReader = Objects.requireNonNull(wholePackageReader);
        this.selectedPackageReader = Objects.requireNonNull(selectedPackageReader);
    }

    public ScenarioCatalogPackageReader.PackageContents read(ScenarioExecutorOptions options) {
        return wholePackageReader.read(manifestPath(options));
    }

    public ScenarioCatalogPackageReader.SelectedPackageContents readSelected(
            ScenarioExecutorOptions options, String workloadPlanId, String faultScenarioId) {
        return selectedPackageReader.read(manifestPath(options), workloadPlanId, faultScenarioId);
    }

    @FunctionalInterface
    interface WholePackageReader {
        ScenarioCatalogPackageReader.PackageContents read(Path manifestPath);
    }

    @FunctionalInterface
    interface SelectedPackageReader {
        ScenarioCatalogPackageReader.SelectedPackageContents read(
                Path manifestPath, String workloadPlanId, String faultScenarioId);
    }

    private Path manifestPath(ScenarioExecutorOptions options) {
        Path configured = Objects.requireNonNull(options.packagePath(), "v4 scenario package path is required");
        return Files.isDirectory(configured)
                ? configured.resolve("scenario-catalog-manifest.json")
                : configured;
    }
}
