package pt.ulisboa.tecnico.socialsoftware.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AdminController {

    private final WebClient webClient;

    private final List<String> services = Arrays.asList( // TODO
            "http://localhost:8082", // Answer
            "http://localhost:8083", // CourseExecution
            "http://localhost:8084", // Question
            "http://localhost:8085", // Quiz
            "http://localhost:8086", // Topic
            "http://localhost:8087", // Tournament
            "http://localhost:8088" // User
    );

    public AdminController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
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
        return Flux.fromIterable(services)
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
        return broadcastPost("/versions/decrement");
    }

    private Mono<String> broadcastGet(String path) {
        return Flux.fromIterable(services)
                .flatMap(serviceUrl -> webClient.get()
                        .uri(serviceUrl + path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(e -> Mono.just("Error on " + serviceUrl + ": " + e.getMessage())))
                .collect(Collectors.joining("\n"));
    }

    private Mono<String> broadcastPost(String path) {
        return Flux.fromIterable(services)
                .flatMap(serviceUrl -> webClient.post()
                        .uri(serviceUrl + path)
                        .retrieve()
                        .bodyToMono(String.class)
                        .onErrorResume(e -> Mono.just("Error on " + serviceUrl + ": " + e.getMessage())))
                .collect(Collectors.joining("\n"));
    }
}
