package pt.ulisboa.tecnico.socialsoftware.ms.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@Profile("remote")
public class EventSubscriberService {
    private static final Logger logger = LoggerFactory.getLogger(EventSubscriberService.class);

    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final Map<String, Class<? extends Event>> subscribedEvents = new HashMap<>();
    private final Map<String, Class<? extends Event>> eventClassesBySimpleName = new HashMap<>();
    private boolean initialized = false;

    @Autowired(required = false)
    private AggregateRepository aggregateRepository;

    public EventSubscriberService(EventService eventService,
                                  MessagingObjectMapperProvider mapperProvider) {
        this.eventService = eventService;
        this.objectMapper = mapperProvider.newMapper();
        scanEventClasses();
    }

    @SuppressWarnings("unchecked")
    private void scanEventClasses() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(Event.class));

        for (BeanDefinition bd : scanner.findCandidateComponents("")) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (Event.class.isAssignableFrom(clazz) && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    eventClassesBySimpleName.put(clazz.getSimpleName(), (Class<? extends Event>) clazz);
                }
            } catch (ClassNotFoundException e) {
                logger.warn("Could not load event class: {}", bd.getBeanClassName());
            }
        }
        logger.info("Scanned event classes: {}", eventClassesBySimpleName.keySet());
    }

    private void initializeSubscribedEvents() {
        if (initialized) {
            return;
        }

        if (aggregateRepository == null) {
            logger.warn("No AggregateRepository found. Cannot initialize subscribed events");
            return;
        }

        Aggregate aggregate = aggregateRepository.findTopByOrderByVersionDesc().orElse(null);
        if (aggregate == null) {
            logger.warn("No aggregate found to initialize subscribed events");
            return;
        }

        Set<EventSubscription> eventSubscriptions = aggregate.getEventSubscriptions();
        for (EventSubscription subscription : eventSubscriptions) {
            String eventType = subscription.getEventType();
            Class<? extends Event> eventClass = eventClassesBySimpleName.get(eventType);
            if (eventClass != null) {
                subscribedEvents.put(eventType, eventClass);
            } else {
                logger.warn("Event class not found for type: {}", eventType);
            }
        }

        initialized = true;
        logger.info("Initialized subscribed events {} from aggregate: {}", subscribedEvents.keySet(), aggregate.getAggregateType());
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
            if (event.getId() != null && !eventService.existsEventById(event.getId())) {
                event.setId(null);
            }
            eventService.saveEvent(event);

            logger.info("Saved event '{}'", eventType);
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}
