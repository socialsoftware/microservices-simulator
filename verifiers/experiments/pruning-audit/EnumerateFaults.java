import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.*;
import java.nio.file.*;
import java.util.*;

/** Enumerate canonical forward faults through the existing package request API. */
public final class EnumerateFaults {
    public static void main(String[] args) throws Exception {
        var json = new ObjectMapper();
        var service = new OnDemandFaultScenarioService();
        var output = new ArrayList<Object>();
        for (String directory : args) {
            Path root = Path.of(directory);
            var requests = new ArrayList<Object>();
            for (String line : Files.readAllLines(root.resolve("workloads.jsonl"))) {
                if (line.isBlank()) continue;
                JsonNode w = json.readTree(line);
                var slots = new TreeMap<String,List<Integer>>();
                int width = 0;
                for (JsonNode step : w.get("schedule")) if (step.has("faultSlot")) {
                    int slot = step.get("faultSlot").asInt();
                    slots.computeIfAbsent(step.get("participant").asText(), k -> new ArrayList<>()).add(slot);
                    width = Math.max(width, slot + 1);
                }
                var vectors = new ArrayList<String>();
                char[] bits = new char[width]; Arrays.fill(bits, '0');
                vectors(new ArrayList<>(slots.values()), 0, bits, vectors);
                for (String vector : vectors) {
                    var r = service.request(new OnDemandFaultScenarioRequest(root.resolve("scenario-catalog-manifest.json"), w.get("id").asText(), vector, "10000"));
                    if (!r.successful() || !r.uncappedScheduleCount().equals(Integer.toString(r.writtenScheduleCount())))
                        throw new IllegalStateException(r.toString());
                    requests.add(r);
                }
            }
            var scenarios = Files.readAllLines(root.resolve("fault-scenarios.jsonl")).stream().filter(s -> !s.isBlank()).toList();
            long controls = scenarios.stream().filter(s -> {try {return !json.readTree(s).get("faultVector").asText().contains("1");} catch(Exception e){throw new RuntimeException(e);}}).count();
            output.add(Map.of("package", root.toString(), "requests", requests, "distinctScenarios", scenarios.size(), "noFaultScenarios", controls));
        }
        System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(output));
    }
    static void vectors(List<List<Integer>> slots, int index, char[] bits, List<String> out) {
        if (index == slots.size()) {out.add(new String(bits));return;}
        vectors(slots,index+1,bits,out);
        for (int slot : slots.get(index)) {bits[slot]='1';vectors(slots,index+1,bits,out);bits[slot]='0';}
    }
}
