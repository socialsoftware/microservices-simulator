package pt.ulisboa.tecnico.socialsoftware.ms.gateway;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Profile("gateway")
@Service
public class DynamicGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicGatewayService.class);

    private final DiscoveryClient discoveryClient;

    @Value("${gateway.service-suffix:-service}")
    private String serviceSuffix;

    @Value("#{'${gateway.exclude-services:gateway,version}'.split(',')}")
    private List<String> excludeServices;

    public DynamicGatewayService(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @PostConstruct
    public void logDiscoveryStatus() {
        logger.info("=== Gateway Service Discovery Status ===");
        logger.info("Discovery client type: {}", discoveryClient.getClass().getSimpleName());

        List<String> allServices = discoveryClient.getServices();
        if (allServices.isEmpty()) {
            logger.info("No services discovered yet. This is normal during startup as services register with the discovery server.");
        } else {
            logger.info("Total services found: {}", allServices.size());
            logger.info("All discovered services: {}", allServices);
        }

        List<String> availableServices = getAvailableServices();
        if (!availableServices.isEmpty()) {
            logger.info("Available broadcast services ({}): {}", availableServices.size(), availableServices);
        }

        String versionUrl = getVersionServiceUrl();
        if (versionUrl != null) {
            logger.info("Version service URL: {}", versionUrl);
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
        return getServiceUrl("version");
    }
}
