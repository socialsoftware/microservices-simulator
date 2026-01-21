package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class EventSubscriberService {
    private static final Logger logger = LoggerFactory.getLogger(EventSubscriberService.class);

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends Event>> subscribedEvents = new HashMap<>();
    private boolean initialized = false;

    public EventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        this.eventRepository = eventRepository;
        this.objectMapper = mapperProvider.newMapper();
    }

    public abstract Aggregate getLatestAggregate();

    public abstract String getEventPackage();

    private void initializeSubscribedEvents() {
        if (initialized) {
            return;
        }

        Aggregate aggregate = getLatestAggregate();
        logger.info("Subscribing to aggregate {}", aggregate);
        if (aggregate == null) {
            logger.warn("No aggregate found to initialize subscribed events");
            return;
        }

        Set<EventSubscription> eventSubscriptions = aggregate.getEventSubscriptions();
        for (EventSubscription subscription : eventSubscriptions) {
            String eventType = subscription.getEventType();
            try {
                String fullClassName = getEventPackage() + "." + eventType;
                Class<?> eventClass = Class.forName(fullClassName);
                if (Event.class.isAssignableFrom(eventClass)) {
                    subscribedEvents.put(eventType, (Class<? extends Event>) eventClass);
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Event class not found for type: {}", eventType);
            }
        }

        initialized = true;
        logger.info("Initialized subscribed events from aggregate: {}", subscribedEvents.keySet());
    }

    public void processEvent(Message<String> message) {
        // Initialize subscribed events on first processEvent call
        initializeSubscribedEvents();

        String eventType = message.getHeaders().get("eventType", String.class);

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
            if (event.getId() != null && !eventRepository.existsById(event.getId())) {
                event.setId(null);
            }
            eventRepository.save(event);

            logger.info("Saved event '{}'", eventType);
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}
