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

public abstract class EventSubscriberService {
    private static final Logger logger = LoggerFactory.getLogger(EventSubscriberService.class);

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public EventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        this.eventRepository = eventRepository;
        this.objectMapper = mapperProvider.newMapper();
    }

    public abstract Map<String, Class<? extends Event>> getSubscribedEvents();

    public void processEvent(Message<String> message) {
        String eventType = message.getHeaders().get("eventType", String.class);

        Map<String, Class<? extends Event>> subscribedEvents = getSubscribedEvents();
        if (!subscribedEvents.containsKey(eventType)) {
            return;
        }

        try {
            String payload = message.getPayload();
            Class<? extends Event> eventClass = subscribedEvents.get(eventType);

            if (eventClass == null) {
                logger.warn("Unknown event type: {}", eventType);
                return;
            }

            Event event = objectMapper.readValue(payload, eventClass);
            event.setPublished(true);
            eventRepository.save(event);

            logger.info("Saved event '{}'", eventType);
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}
