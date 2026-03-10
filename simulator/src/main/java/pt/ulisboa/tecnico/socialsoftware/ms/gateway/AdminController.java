package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Profile("gateway")
@RestController
public class AdminController {

    private final RestClient restClient;
    private final DynamicGatewayService dynamicGatewayService;

    public AdminController(RestClient.Builder restClientBuilder, DynamicGatewayService dynamicGatewayService) {
        this.restClient = restClientBuilder.build();
        this.dynamicGatewayService = dynamicGatewayService;
    }

    @GetMapping("/scheduler/start")
    public String startScheduler() {
        return broadcastGet("/scheduler/start");
    }

    @GetMapping("/scheduler/stop")
    public String stopScheduler() {
        return broadcastGet("/scheduler/stop");
    }

    @GetMapping("/traces/start")
    public String startTraces() {
        List<String> services = dynamicGatewayService.getAvailableServices();
        if (services.isEmpty()) {
            return "No services available";
        }

        String firstService = services.getFirst();

        try {
            String rootResponse = restClient.get()
                    .uri(firstService + "/traces/createRoot")
                    .retrieve()
                    .body(String.class);

            if (rootResponse == null) {
                return "Failed to get response from createRoot";
            }

            String[] parts = rootResponse.split(":");
            if (parts.length < 2) {
                return "Failed to parse traceId:spanId from createRoot: " + rootResponse;
            }
            String traceId = parts[0];
            String spanId = parts[1];

            String responses = services.stream()
                    .map(serviceUrl -> {
                        try {
                            return restClient.get()
                                    .uri(serviceUrl + "/traces/start?traceId=" + traceId + "&spanId=" + spanId)
                                    .retrieve()
                                    .body(String.class);
                        } catch (Exception e) {
                            return "Error on " + serviceUrl + ": " + e.getMessage();
                        }
                    })
                    .collect(Collectors.joining("\n"));

            return "Root: " + rootResponse + "\n" + responses;
        } catch (Exception e) {
            return "Error creating root: " + e.getMessage();
        }
    }

    @GetMapping("/traces/end")
    public String stopTraces() {
        return broadcastGet("/traces/end");
    }

    @GetMapping("/traces/flush")
    public String flushTraces() {
        return broadcastGet("/traces/flush");
    }

    @PostMapping("/behaviour/load")
    public String loadBehaviour(@RequestParam String dir) {
        return dynamicGatewayService.getAvailableServices().stream()
                .map(serviceUrl -> {
                    try {
                        return restClient.post()
                                .uri(serviceUrl + "/behaviour/load?dir=" + dir)
                                .retrieve()
                                .body(String.class);
                    } catch (Exception e) {
                        return "Error on " + serviceUrl + ": " + e.getMessage();
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    @GetMapping("/behaviour/clean")
    public String cleanBehaviour() {
        return broadcastGet("/behaviour/clean");
    }

    @PostMapping("/versions/decrement")
    public String decrementVersion() {
        String versionServiceUrl = dynamicGatewayService.getVersionServiceUrl();
        try {
            return restClient.post()
                    .uri(versionServiceUrl + "/versions/decrement")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "Error on " + versionServiceUrl + ": " + e.getMessage();
        }
    }

    private String broadcastGet(String path) {
        return dynamicGatewayService.getAvailableServices().stream()
                .map(serviceUrl -> {
                    try {
                        return restClient.get()
                                .uri(serviceUrl + path)
                                .retrieve()
                                .body(String.class);
                    } catch (Exception e) {
                        return "Error on " + serviceUrl + ": " + e.getMessage();
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    private String broadcastPost(String path) {
        return dynamicGatewayService.getAvailableServices().stream()
                .map(serviceUrl -> {
                    try {
                        return restClient.post()
                                .uri(serviceUrl + path)
                                .retrieve()
                                .body(String.class);
                    } catch (Exception e) {
                        return "Error on " + serviceUrl + ": " + e.getMessage();
                    }
                })
                .collect(Collectors.joining("\n"));
    }
}
