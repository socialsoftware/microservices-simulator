package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Profile("gateway")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = "org.springframework.cloud.gateway.route.RouteDefinitionLocator")
public class ServiceDiscoveryRouteDefinitionLocator implements RouteDefinitionLocator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryRouteDefinitionLocator.class);

    private final DynamicGatewayService dynamicGatewayService;
    private final Environment environment;
    private final WebClient webClient;

    public ServiceDiscoveryRouteDefinitionLocator(DynamicGatewayService dynamicGatewayService,
                                                  Environment environment,
                                                  WebClient.Builder webClientBuilder) {
        this.dynamicGatewayService = dynamicGatewayService;
        this.environment = environment;
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        List<String> serviceUrls = dynamicGatewayService.getAvailableServices();
        return Flux.fromIterable(serviceUrls)
                .flatMap(this::fetchRoutes)
                .map(this::toRouteDefinition);
    }

    private Flux<Map<String, Object>> fetchRoutes(String serviceUrl) {
        return webClient.get()
                .uri(serviceUrl + "/routes")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(e -> {
                    logger.warn("Failed to fetch routes from {}: {}", serviceUrl, e.getMessage());
                    return Flux.empty();
                });
    }

    @SuppressWarnings("unchecked")
    private RouteDefinition toRouteDefinition(Map<String, Object> map) {
        RouteDefinition def = new RouteDefinition();
        def.setId((String) map.getOrDefault("id", UUID.randomUUID().toString()));

        Object uriObj = map.get("uri");
        if (uriObj == null) {
            throw new IllegalArgumentException("Route definition missing 'uri' field for id: " + def.getId());
        }
        String rawUri = environment.resolvePlaceholders(uriObj.toString());
        def.setUri(URI.create(rawUri));

        if (map.containsKey("order")) {
            def.setOrder((Integer) map.get("order"));
        }

        List<Object> predicates = (List<Object>) map.getOrDefault("predicates", Collections.emptyList());
        def.setPredicates(predicates.stream()
                .map(p -> new PredicateDefinition(environment.resolvePlaceholders(p.toString())))
                .collect(Collectors.toList()));

        List<Object> filters = (List<Object>) map.getOrDefault("filters", Collections.emptyList());
        def.setFilters(filters.stream()
                .map(f -> new FilterDefinition(environment.resolvePlaceholders(f.toString())))
                .collect(Collectors.toList()));

        return def;
    }
}
