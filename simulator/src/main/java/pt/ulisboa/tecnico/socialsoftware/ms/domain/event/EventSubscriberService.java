package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;

import java.util.Map;
import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class EventSubscriberService {
    private static final Logger logger = LoggerFactory.getLogger(EventSubscriberService.class);

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends Event>> eventClassMap;

    public EventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider,
            Map<String, Class<? extends Event>> eventClassMap) {
        this.eventRepository = eventRepository;
        this.objectMapper = mapperProvider.newMapper();
        this.eventClassMap = eventClassMap;
    }

    @Bean
    public Consumer<Message<String>> eventChannel() {
        return this::processEvent;
    }

    private void processEvent(Message<String> message) {
        String eventType = message.getHeaders().get("eventType", String.class);

        try {
            String payload = message.getPayload();
            Class<? extends Event> eventClass = eventClassMap.get(eventType);

            if (eventClass == null) {
                logger.warn("Unknown event type: {}", eventType);
                return;
            }

            // Save the event once - all subscribing aggregates will process it
            Event event = objectMapper.readValue(payload, eventClass);
            event.setPublished(true);
            eventRepository.save(event);

            logger.info("Saved event '{}'", eventType);
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}
