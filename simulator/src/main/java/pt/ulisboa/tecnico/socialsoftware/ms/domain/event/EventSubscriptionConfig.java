package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "event")
public class EventSubscriptionConfig {
    private Map<String, List<String>> subscriptions = new HashMap<>();

    public Map<String, List<String>> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(Map<String, List<String>> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Set<String> getEventTypes() {
        return subscriptions.keySet();
    }

    public Set<String> getAggregateTypes(String eventType) {
        List<String> aggregates = subscriptions.get(eventType);
        return aggregates != null ? new HashSet<>(aggregates) : Collections.emptySet();
    }
}
