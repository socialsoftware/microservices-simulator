package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Profile("gateway")
@RestController
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);


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
    public ResponseEntity<String> startTraces() {
        List<String> services = dynamicGatewayService.getAvailableServices();
        logger.info("Available services: {}", services);
        if (services.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No services available");
        }

        String firstService = services.getFirst();
        String traceId;
        String spanId;

        try {
            String rootResponse = restClient.get()
                    .uri(firstService + "/traces/createRoot")
                    .retrieve()
                    .body(String.class);

            if (rootResponse == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Failed to get response from createRoot on " + firstService);
            }

            String[] parts = rootResponse.split(":");
            if (parts.length < 2) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Failed to parse traceId:spanId from createRoot: " + rootResponse);
            }
            traceId = parts[0];
            spanId = parts[1];
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error creating root on " + firstService + ": " + e.getMessage());
        }

        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        for (String serviceUrl : services) {
            try {
                String response = restClient.get()
                        .uri(serviceUrl + "/traces/start?traceId=" + traceId + "&spanId=" + spanId)
                        .retrieve()
                        .body(String.class);
                successes.add(serviceUrl + ": " + response);
            } catch (Exception e) {
                String error = "Error on " + serviceUrl + ": " + e.getMessage();
                logger.error(error);
                errors.add(error);
            }
        }

        String body = "Root: " + traceId + ":" + spanId + "\n"
                + String.join("\n", successes);

        if (!errors.isEmpty()) {
            body += "\nErrors:\n" + String.join("\n", errors);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }

        return ResponseEntity.ok(body);
    }

    @GetMapping("/traces/end")
    public ResponseEntity<String> stopTraces() {
        return broadcastGetWithStatus("/traces/end");
    }

    @GetMapping("/traces/flush")
    public ResponseEntity<String> flushTraces() {
        return broadcastGetWithStatus("/traces/flush");
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

    private ResponseEntity<String> broadcastGetWithStatus(String path) {
        List<String> services = dynamicGatewayService.getAvailableServices();
        if (services.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No services available");
        }

        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();

        for (String serviceUrl : services) {
            try {
                String response = restClient.get()
                        .uri(serviceUrl + path)
                        .retrieve()
                        .body(String.class);
                successes.add(serviceUrl + ": " + response);
            } catch (Exception e) {
                String error = "Error on " + serviceUrl + ": " + e.getMessage();
                logger.error(error);
                errors.add(error);
            }
        }

        String body = String.join("\n", successes);

        if (!errors.isEmpty()) {
            body += (body.isEmpty() ? "" : "\n") + "Errors:\n" + String.join("\n", errors);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
        }

        return ResponseEntity.ok(body);
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
