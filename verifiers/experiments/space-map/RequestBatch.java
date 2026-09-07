import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.OnDemandFaultScenarioRequest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.OnDemandFaultScenarioService;
import java.nio.file.Files;
import java.nio.file.Path;

/** Experiment orchestration only; calls the existing request service unchanged. */
class RequestBatch {
    public static void main(String[] args) throws Exception {
        var mapper = new ObjectMapper();
        var service = new OnDemandFaultScenarioService();
        try (var output = Files.newBufferedWriter(Path.of(args[1]))) {
            for (var request : mapper.readTree(Path.of(args[0]).toFile())) {
                var result = service.request(new OnDemandFaultScenarioRequest(
                    Path.of(request.get("manifest").asText()), request.get("workloadId").asText(),
                    request.get("vector").asText(), "10000"));
                output.write(mapper.writeValueAsString(result));
                output.newLine();
                output.flush();
                System.out.println(result.status() + " " + result.workloadPlanId() + " "
                    + result.assignedVector() + " " + result.writtenScheduleCount());
            }
        }
    }
}
