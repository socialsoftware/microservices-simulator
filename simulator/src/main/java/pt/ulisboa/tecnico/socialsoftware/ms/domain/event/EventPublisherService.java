package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;

import java.util.List;

@Service
@Profile({ "stream", "grpc" })
public class EventPublisherService {
    private static final Logger logger = LoggerFactory.getLogger(EventPublisherService.class);
    private static final String EVENT_CHANNEL = "event-channel";

    private final EventRepository eventRepository;
    private final StreamBridge streamBridge;
    private final ObjectMapper objectMapper;

    public EventPublisherService(EventRepository eventRepository,
            StreamBridge streamBridge,
            MessagingObjectMapperProvider mapperProvider) {
        this.eventRepository = eventRepository;
        this.streamBridge = streamBridge;
        this.objectMapper = mapperProvider.newMapper();
    }

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {
        List<Event> pending = eventRepository.findAll().stream()
                .filter(e -> !e.isPublished())
                .toList();

        for (Event event : pending) {
            publishEvent(event);
        }
    }

    private void publishEvent(Event event) {
        String eventSimpleName = event.getClass().getSimpleName();
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
                logger.info("Published event '{}' to '{}'", eventSimpleName, EVENT_CHANNEL);
            } else {
                logger.error("Failed to publish event '{}' to '{}'", eventSimpleName, EVENT_CHANNEL);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error serializing event: {}", e.getMessage());
        }
    }
}