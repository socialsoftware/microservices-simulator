package pt.ulisboa.tecnico.socialsoftware.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private List<String> services;

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }
}
