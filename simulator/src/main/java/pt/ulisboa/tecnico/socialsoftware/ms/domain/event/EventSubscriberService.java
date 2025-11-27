package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;

import java.util.function.Consumer;

@Profile("stream")
public abstract class EventSubscriberService implements Consumer<Message<String>> {
    private static final Logger logger = LoggerFactory.getLogger(EventSubscriberService.class);

    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final String aggregateName;

    public EventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider,
            String aggregateName) {
        this.eventRepository = eventRepository;
        this.objectMapper = mapperProvider.newMapper();
        this.aggregateName = aggregateName;
    }

    @Override
    public void accept(Message<String> message) {
        try {
            String payload = message.getPayload();
            String eventType = message.getHeaders().get("eventType", String.class);

            logger.info("{} received event: type='{}', payload='{}'",
                    aggregateName, eventType, payload);

            Class<? extends Event> eventClass = getEventClass(eventType);
            if (eventClass != null) {
                Event event = objectMapper.readValue(payload, eventClass);
                event.setPublished(true);
                eventRepository.save(event);

                logger.info("{} saved event: type='{}', id='{}'",
                        aggregateName, eventType, event.getId());
            } else {
                logger.warn("{} received unknown event type: {}", aggregateName, eventType);
            }
        } catch (Exception e) {
            logger.error("{} error processing event: {}", aggregateName, e.getMessage(), e);
        }
    }

    protected abstract Class<? extends Event> getEventClass(String eventType);
}
