package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

public final class DynamicInputAttributionHolder {
    private static volatile LoadedInputMap loadedInputMap = LoadedInputMap.disabled();

    private DynamicInputAttributionHolder() {
    }

    public static void setInputMap(DynamicInputMap inputMap, boolean active) {
        loadedInputMap = new LoadedInputMap(inputMap == null ? DynamicInputMap.empty() : inputMap, active);
    }

    public static void clear() {
        loadedInputMap = LoadedInputMap.disabled();
    }

    public static DynamicInputAttribution resolve(String functionalityClassFqn, String stepName) {
        LoadedInputMap loaded = loadedInputMap;
        if (!loaded.active()) {
            return DynamicInputAttribution.disabled();
        }
        return loaded.inputMap().resolve(DynamicEvidenceTestContext.current().orElse(null), functionalityClassFqn, stepName);
    }

    private record LoadedInputMap(DynamicInputMap inputMap, boolean active) {
        private static LoadedInputMap disabled() {
            return new LoadedInputMap(DynamicInputMap.empty(), false);
        }
    }
}
