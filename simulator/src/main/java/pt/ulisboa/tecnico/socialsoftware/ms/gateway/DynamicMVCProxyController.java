package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@Profile("gateway")
public class DynamicMVCProxyController {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMVCProxyController.class);
    private final DynamicGatewayService dynamicGatewayService;
    private final RestClient restClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Map to hold dynamic routes: <Pattern, TargetURI>
    private final Map<String, String> resolvedRoutes = new ConcurrentHashMap<>();

    public DynamicMVCProxyController(DynamicGatewayService dynamicGatewayService, RestClient.Builder restClientBuilder) {
        this.dynamicGatewayService = dynamicGatewayService;
        this.restClient = restClientBuilder.build();
    }

    // Refresh routes every 15 seconds to keep up with dynamic service changes
    @Scheduled(fixedRate = 15000)
    public void refreshRoutes() {
        logger.info("Refreshing routes...");
        List<String> services = dynamicGatewayService.getAvailableServices();
        Map<String, String> newRoutes = new ConcurrentHashMap<>();

        for (String serviceUrl : services) {
            try {
                List<Map<String, Object>> routes = restClient.get()
                        .uri(serviceUrl + "/routes")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {
                        });

                if (routes != null) {
                    for (Map<String, Object> routeMap : routes) {
                        @SuppressWarnings("unchecked")
                        List<Object> predicates = (List<Object>) routeMap.getOrDefault("predicates", Collections.emptyList());
                        for (Object predicateObj : predicates) {
                            String predicate = predicateObj.toString();
                            if (predicate.startsWith("Path=")) {
                                String pathPatternString = predicate.substring("Path=".length());
                                pathPatternString = pathPatternString.replaceAll("[\\[\\]]", "");
                                for (String pathPattern : pathPatternString.split(",")) {
                                    pathPattern = pathPattern.trim();
                                    logger.info("Found route: {} -> {}", pathPattern, serviceUrl);
                                    newRoutes.put(pathPattern, serviceUrl);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to fetch routes from {}: {}", serviceUrl, e.getMessage());
            }
        }

        resolvedRoutes.clear();
        resolvedRoutes.putAll(newRoutes);
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request, HttpServletResponse response) throws URISyntaxException, IOException {
        String requestUri = request.getRequestURI();
        
        // Find matching route
        String targetBaseUrl = null;
        String bestMatchPattern = null;
        for (Map.Entry<String, String> entry : resolvedRoutes.entrySet()) {
            String pattern = entry.getKey();
            if (pathMatcher.match(pattern, requestUri)) {
                if (bestMatchPattern == null || pathMatcher.getPatternComparator(requestUri).compare(pattern, bestMatchPattern) < 0) {
                    bestMatchPattern = pattern;
                    targetBaseUrl = entry.getValue();
                }
            }
        }

        if (targetBaseUrl == null) {
            return ResponseEntity.notFound().build();
        }

        String targetUrl = targetBaseUrl + requestUri;
        if (request.getQueryString() != null) {
            targetUrl += "?" + request.getQueryString();
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        byte[] body = request.getInputStream().readAllBytes();

        RestClient.RequestBodySpec requestSpec = restClient.method(method)
                .uri(new URI(targetUrl))
                .body(body);

        // Copy Headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host") && !headerName.equalsIgnoreCase("content-length")) {
                Enumeration<String> headers = request.getHeaders(headerName);
                while (headers.hasMoreElements()) {
                    requestSpec.header(headerName, headers.nextElement());
                }
            }
        }

        try {
            return requestSpec.retrieve()
                    .toEntity(byte[].class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("Proxy error ({} ) from {}: {}", e.getStatusCode(), targetUrl, e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (RestClientException e) {
            logger.error("Proxy connection error to {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(502).build();
        }
    }
}
