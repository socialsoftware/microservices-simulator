package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;

@Component
@Profile("stream")
public class QuizEventSubscriberService extends EventSubscriberService {

    public QuizEventSubscriberService(EventRepository eventRepository, MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Map<String, Class<? extends Event>> getSubscribedEvents() {
        return Map.of(
            DeleteCourseExecutionEvent.class.getSimpleName(), DeleteCourseExecutionEvent.class
        );
    }

    @Bean
    public Consumer<Message<String>> quizEventSubscriber() {
        return this::processEvent;
    }
}
