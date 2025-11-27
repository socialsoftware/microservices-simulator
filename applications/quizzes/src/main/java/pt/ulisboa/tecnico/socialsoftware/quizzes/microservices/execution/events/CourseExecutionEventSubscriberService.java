package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class CourseExecutionEventSubscriberService extends EventSubscriberService {

    public CourseExecutionEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider, "courseexecution");
    }

    @Bean
    public Consumer<Message<String>> courseExecutionEventChannel() {
        return this;
    }

    @Override
    protected Class<? extends Event> getEventClass(String eventType) {
        return switch (eventType) {
            case "DeleteUserEvent" -> DeleteUserEvent.class;
            default -> null;
        };
    }
}
