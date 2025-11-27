package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;

import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class TopicEventSubscriberService extends EventSubscriberService {

    public TopicEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider, "topic");
    }

    @Bean
    public Consumer<Message<String>> topicEventChannel() {
        return this;
    }

    @Override
    protected Class<? extends Event> getEventClass(String eventType) {
        // Topic doesn't subscribe to any events currently
        return null;
    }
}
