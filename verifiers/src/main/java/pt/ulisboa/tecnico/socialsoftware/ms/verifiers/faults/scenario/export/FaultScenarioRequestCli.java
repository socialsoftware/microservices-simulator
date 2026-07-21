package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FaultScenarioRequestCli {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FaultScenarioRequestCli() {
    }

    public static void main(String[] args) throws Exception {
        System.exit(run(args, System.out, new OnDemandFaultScenarioService()));
    }

    static int run(String[] args, PrintStream output, OnDemandFaultScenarioService service) throws Exception {
        Map<String, String> options = parse(args);
        require(options, "manifest-path");
        require(options, "workload-plan-id");
        require(options, "fault-vector");
        OnDemandFaultScenarioResult result = service.request(new OnDemandFaultScenarioRequest(
                Path.of(options.get("manifest-path")),
                options.get("workload-plan-id"),
                options.get("fault-vector"),
                options.get("recovery-schedule-cap")));
        output.println(OBJECT_MAPPER.writeValueAsString(result));
        return result.successful() ? 0 : 1;
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (int index = 0; index < args.length; index++) {
            if (!args[index].startsWith("--")) {
                continue;
            }
            String key = args[index].substring(2);
            String value = index + 1 < args.length && !args[index + 1].startsWith("--")
                    ? args[++index]
                    : "true";
            parsed.put(key, value);
        }
        return parsed;
    }

    private static void require(Map<String, String> options, String key) {
        if (!options.containsKey(key) || options.get(key).isBlank()) {
            throw new IllegalArgumentException("Missing required --" + key);
        }
    }
}
