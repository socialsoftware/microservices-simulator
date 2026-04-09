package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandHandlerBuildingBlock extends BuildingBlock {
    private final String aggregateTypeName;
    private final Map<String, CommandDispatchInfo> commandDispatch = new LinkedHashMap<>();

    public CommandHandlerBuildingBlock(Path file, String packageName, String name, String aggregateTypeName) {
        super(file, packageName, name);
        this.aggregateTypeName = aggregateTypeName;
    }

    public void addCommandDispatch(String commandType, CommandDispatchInfo info) {
        commandDispatch.put(commandType, info);
    }

    public Map<String, CommandDispatchInfo> getCommandDispatch() {
        return Collections.unmodifiableMap(commandDispatch);
    }

    public String getAggregateTypeName() {
        return aggregateTypeName;
    }
}
