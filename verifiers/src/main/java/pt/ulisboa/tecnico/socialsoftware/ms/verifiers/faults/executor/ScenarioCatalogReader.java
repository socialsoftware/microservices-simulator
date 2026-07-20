package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ScenarioCatalogReader {
    private final ScenarioCatalogPackageReader packageReader;

    public ScenarioCatalogReader() {
        this(new ScenarioCatalogPackageReader());
    }

    ScenarioCatalogReader(ScenarioCatalogPackageReader packageReader) {
        this.packageReader = Objects.requireNonNull(packageReader);
    }

    public ScenarioCatalogPackageReader.PackageContents read(ScenarioExecutorOptions options) {
        Path configured = Objects.requireNonNull(options.packagePath(), "v3 scenario package path is required");
        Path manifest = Files.isDirectory(configured)
                ? configured.resolve("scenario-catalog-manifest.json")
                : configured;
        return packageReader.read(manifest);
    }
}
