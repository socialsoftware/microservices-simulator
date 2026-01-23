package pt.ulisboa.tecnico.socialsoftware.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
public class AdminController {

    private final WebClient webClient;
    private final DynamicGatewayService dynamicGatewayService;

    public AdminController(WebClient.Builder webClientBuilder, DynamicGatewayService dynamicGatewayService) {
        this.webClient = webClientBuilder.build();
        this.dynamicGatewayService = dynamicGatewayService;
    }

    @GetMapping("/scheduler/start")
    public Mono<String> startScheduler() {
        return broadcastGet("/scheduler/start");
    }

    @GetMapping("/scheduler/stop")
    public Mono<String> stopScheduler() {
        return broadcastGet("/scheduler/stop");
    }

    @GetMapping("/traces/start")
    public Mono<String> startTraces() {
        return broadcastGet("/traces/start");
    }

    @GetMapping("/traces/end")
    public Mono<String> stopTraces() {
        return broadcastGet("/traces/end");
    }

    @GetMapping("/traces/flush")
    public Mono<String> flushTraces() {
        return broadcastGet("/traces/flush");
    }

    @PostMapping("/behaviour/load")
    public Mono<String> loadBehaviour(@RequestParam String dir) {
        return Flux.fromIterable(dynamicGatewayService.getAvailableServices())
                .flatMap(serviceUrl -> webClient.post()
                        .uri(serviceUrl + "/behaviour/load?dir=" + dir)
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(e -> Mono.just("Error on " + serviceUrl + ": " + e.getMessage())))
                .collect(Collectors.joining("\n"));
    }

    @GetMapping("/behaviour/clean")
    public Mono<String> cleanBehaviour() {
        return broadcastGet("/behaviour/clean");
    }

    @PostMapping("/versions/decrement")
    public Mono<String> decrementVersion() {
        String versionServiceUrl = dynamicGatewayService.getVersionServiceUrl();
        return webClient.post()
                .uri(versionServiceUrl + "/versions/decrement")
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> Mono.just("Error on " + versionServiceUrl + ": " + e.getMessage()));
    }

    private Mono<String> broadcastGet(String path) {
        return Flux.fromIterable(dynamicGatewayService.getAvailableServices())
                .flatMap(serviceUrl -> webClient.get()
                        .uri(serviceUrl + path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(e -> Mono.just("Error on " + serviceUrl + ": " + e.getMessage())))
                .collect(Collectors.joining("\n"));
    }

    private Mono<String> broadcastPost(String path) {
        return Flux.fromIterable(dynamicGatewayService.getAvailableServices())
                .flatMap(serviceUrl -> webClient.post()
                        .uri(serviceUrl + path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(e -> Mono.just("Error on " + serviceUrl + ": " + e.getMessage())))
                .collect(Collectors.joining("\n"));
    }
}
