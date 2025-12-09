package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import java.util.function.Consumer;
import java.util.Map;
import java.util.Collections;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;

@Component
@Profile("stream")
public class CourseEventSubscriberService extends EventSubscriberService {

    public CourseEventSubscriberService(EventRepository eventRepository, MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Map<String, Class<? extends Event>> getSubscribedEvents() {
        return Collections.emptyMap();
    }

    @Bean
    public Consumer<Message<String>> courseEventSubscriber() {
        return this::processEvent;
    }
}
