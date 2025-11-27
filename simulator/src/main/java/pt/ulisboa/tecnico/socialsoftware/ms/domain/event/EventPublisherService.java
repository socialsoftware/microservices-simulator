package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;

import java.util.List;
import java.util.Set;

@Profile("stream")
public abstract class EventPublisherService {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);

    private final EventRepository eventRepository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;
    private final EventSubscriptionConfig eventSubscriptionConfig;
    private final String aggregateName;

    public EventPublisherService(EventRepository eventRepository,
            StreamBridge streamBridge,
            MessagingObjectMapperProvider mapperProvider,
            EventSubscriptionConfig eventSubscriptionConfig,
            String aggregateName) {
        this.eventRepository = eventRepository;
        this.streamBridge = streamBridge;
        this.objectMapper = mapperProvider.newMapper();
        this.eventSubscriptionConfig = eventSubscriptionConfig;
        this.aggregateName = aggregateName;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        eventSubscriptionConfig.getEventTypes().forEach(eventType -> {
            Set<String> aggregateTypes = eventSubscriptionConfig.getAggregateTypes(eventType);
            if (!aggregateTypes.isEmpty()) {
                List<String> destinations = aggregateTypes.stream()
                        .filter(aggregateType -> !aggregateType.equalsIgnoreCase(aggregateName))
                        .map(this::buildDestination)
                        .toList();

                if (!destinations.isEmpty()) {
                    publishPendingEventsBySimpleName(eventType, destinations);
                }
            }
        });
    }

    protected abstract String getAggregatePackageName();

    private void publishPendingEventsBySimpleName(String eventSimpleName, List<String> destinations) {
        String packageName = getAggregatePackageName();
        List<Event> pending = eventRepository.findAll().stream()
                .filter(e -> e.getClass().getSimpleName().equals(eventSimpleName))
                .filter(e -> e.getClass().getPackage().getName().startsWith(packageName))
                .filter(e -> !e.isPublished())
                .toList();

        for (Event event : pending) {
            try {
                String json = objectMapper.writeValueAsString(event);
                boolean allSent = true;

                for (String destination : destinations) {
                    boolean sent = streamBridge.send(destination,
                            MessageBuilder.withPayload(json)
                                    .setHeader("contentType", "application/json")
                                    .setHeader("eventType", eventSimpleName)
                                    .build());

                    if (sent) {
                        logger.info("{}: Published event '{}' to '{}'", aggregateName, eventSimpleName, destination);
                    } else {
                        logger.error("{}: Failed to publish event '{}' to '{}'", aggregateName, eventSimpleName, destination);
                        allSent = false;
                    }
                }

                if (allSent) {
                    event.setPublished(true);
                    eventRepository.save(event);
                    logger.info("Event '{}' marked as published after sending to all destinations", eventSimpleName);
                }
            } catch (JsonProcessingException e) {
                logger.error("Error serializing event: {}", e.getMessage());
            }
        }
    }

    private String buildDestination(String aggregateType) {
        return aggregateType.toLowerCase() + "-event-channel";
    }
}