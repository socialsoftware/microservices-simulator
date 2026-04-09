package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.nio.file.Path;

public abstract class BuildingBlock {
    private final Path file;
    private final String packageName;
    private final String fqn;

    protected BuildingBlock(Path file, String packageName, String fqn) {
        this.file = file;
        this.packageName = packageName;
        this.fqn = fqn;
    }

    public Path getFile() {
        return file;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getFqn() {
        return fqn;
    }
}
