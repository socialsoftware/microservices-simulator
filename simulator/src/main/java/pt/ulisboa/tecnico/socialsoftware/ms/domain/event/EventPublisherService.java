package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;

import java.util.List;

@Profile("stream")
public class EventPublisherService {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);
    private static final String EVENT_CHANNEL = "event-channel";

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
        // Get all event types that have subscribers (other than self)
        eventSubscriptionConfig.getEventTypes().forEach(eventType -> {
            boolean hasOtherSubscribers = eventSubscriptionConfig.getAggregateTypes(eventType).stream()
                    .anyMatch(aggregateType -> !aggregateType.equalsIgnoreCase(aggregateName));

            if (hasOtherSubscribers) {
                publishPendingEventsBySimpleName(eventType);
            }
        });
    }

    private void publishPendingEventsBySimpleName(String eventSimpleName) {
        List<Event> pending = eventRepository.findAll().stream()
                .filter(e -> e.getClass().getSimpleName().equals(eventSimpleName))
                .filter(e -> e.getClass().getPackage().getName().contains("." + this.aggregateName + ".")) // TODO
                .filter(e -> !e.isPublished())
                .toList();

        for (Event event : pending) {
            try {
                String json = objectMapper.writeValueAsString(event);

                boolean sent = streamBridge.send(EVENT_CHANNEL,
                        MessageBuilder.withPayload(json)
                                .setHeader("contentType", "application/json")
                                .setHeader("eventType", eventSimpleName)
                                .build());

                if (sent) {
                    event.setPublished(true);
                    eventRepository.save(event);
                    logger.info("{}: Published event '{}' to '{}'", aggregateName, eventSimpleName, EVENT_CHANNEL);
                } else {
                    logger.error("{}: Failed to publish event '{}' to '{}'", aggregateName, eventSimpleName, EVENT_CHANNEL);
                }
            } catch (JsonProcessingException e) {
                logger.error("Error serializing event: {}", e.getMessage());
            }
        }
    }
}