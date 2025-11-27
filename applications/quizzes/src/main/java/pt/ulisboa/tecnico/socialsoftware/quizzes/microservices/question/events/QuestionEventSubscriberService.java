package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;

import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class QuestionEventSubscriberService extends EventSubscriberService {

    public QuestionEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider, "question");
    }

    @Bean
    public Consumer<Message<String>> questionEventChannel() {
        return this;
    }

    @Override
    protected Class<? extends Event> getEventClass(String eventType) {
        return switch (eventType) {
            case "UpdateTopicEvent" -> UpdateTopicEvent.class;
            case "DeleteTopicEvent" -> DeleteTopicEvent.class;
            default -> null;
        };
    }
}
