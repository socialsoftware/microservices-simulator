package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public abstract class BuildingBlock {
    private final Path file;
    private final String packageName;
    private final String name;
    private final Set<String> inputKeys;

    public BuildingBlock(Path file, String packageName, String names) {
        this(file, packageName, names, new HashSet<>());
    }

    public BuildingBlock(Path file, String packageName, String name, Set<String> inputKeys) {
        this.file = file;
        this.packageName = packageName;
        this.name = name;
        this.inputKeys = inputKeys;
    }

    public boolean bind(String inputKey) {
        return this.inputKeys.add(inputKey);
    }

    public Path getFile() {
        return file;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public Set<String> getInputKeys() {
        return inputKeys;
    }
}
