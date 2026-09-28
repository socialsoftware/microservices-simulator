import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.FaultScenarioCountWriter;
import java.nio.file.*;

/** Reuses a frozen extraction. Args: model.json output-dir max-sagas strategy schedule max-deliveries [max-states]. */
public final class CountFaultScenarios {
    public static void main(String[] args) throws Exception {
        var json = new ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var model = json.readValue(Path.of(args[0]).toFile(), ScenarioModelAdapterResult.class);
        var config = new ScenarioGeneratorConfig(false, ScenarioGeneratorConfig.GenerationStrategy.valueOf(args[3]),
                ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY, false, Integer.parseInt(args[2]), 0, Integer.MAX_VALUE, 0,
                true, ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.valueOf(args[4]), 1234L, 1000000, Integer.parseInt(args[5]));
        long start = System.nanoTime();
        var report = FaultScenarioCountWriter.write(model, config, Path.of(args[1]), args.length > 6 ? Long.parseLong(args[6]) : 2000000, 100000);
        System.out.println(json.writeValueAsString(report));
        System.out.println("elapsedSeconds=" + (System.nanoTime()-start)/1e9);
    }
}
