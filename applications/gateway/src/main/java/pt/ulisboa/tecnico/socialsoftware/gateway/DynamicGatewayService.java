package pt.ulisboa.tecnico.socialsoftware.gateway;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DynamicGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicGatewayService.class);

    private final DiscoveryClient discoveryClient;

    @Value("${gateway.service-suffix:-service}")
    private String serviceSuffix;

    @Value("#{'${gateway.exclude-services:gateway}'.split(',')}")
    private List<String> excludeServices;

    @Value("${version-service}")
    private String versionService;

    public DynamicGatewayService(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @PostConstruct
    public void logDiscoveryStatus() {
        logger.info("=== Gateway Service Discovery Status ===");

        List<String> allServices = discoveryClient.getServices();
        logger.info("Discovery client type: {}", discoveryClient.getClass().getSimpleName());
        logger.info("Total services found: {}", allServices.size());

        if (allServices.isEmpty()) {
            logger.warn("No services discovered! Check if service discovery is configured correctly.");
        } else {
            logger.info("All discovered services: {}", allServices);
        }

        List<String> availableServices = getAvailableServices();
        if (availableServices.isEmpty()) {
            logger.warn("No broadcast services available after filtering (suffix: '{}', excluded: {})",
                    serviceSuffix, excludeServices);
        } else {
            logger.info("Available broadcast services ({}): {}", availableServices.size(), availableServices);
        }

        String versionUrl = getVersionServiceUrl();
        if (versionUrl != null) {
            logger.info("Version service URL: {}", versionUrl);
        } else {
            logger.warn("Version service not found!");
        }

        logger.info("=========================================");
    }

    public List<String> getAvailableServices() {
        return discoveryClient.getServices().stream()
                .filter(serviceId -> serviceId.endsWith(serviceSuffix))
                .filter(serviceId -> !excludeServices.contains(serviceId))
                .map(this::getServiceUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String getServiceUrl(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances.isEmpty()) {
            return null;
        }
        return instances.getFirst().getUri().toString();
    }

    public String getVersionServiceUrl() {
        return versionService;
    }
}
