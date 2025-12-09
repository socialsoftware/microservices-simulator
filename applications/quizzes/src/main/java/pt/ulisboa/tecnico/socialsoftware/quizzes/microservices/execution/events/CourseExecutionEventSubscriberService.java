package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import java.util.function.Consumer;
import java.util.Map;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

@Component
@Profile("stream")
public class CourseExecutionEventSubscriberService extends EventSubscriberService {

    public CourseExecutionEventSubscriberService(EventRepository eventRepository, MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Map<String, Class<? extends Event>> getSubscribedEvents() {
        return Map.of(
            DeleteUserEvent.class.getSimpleName(), DeleteUserEvent.class
        );
    }

    @Bean
    public Consumer<Message<String>> courseExecutionEventSubscriber() {
        return this::processEvent;
    }
}
