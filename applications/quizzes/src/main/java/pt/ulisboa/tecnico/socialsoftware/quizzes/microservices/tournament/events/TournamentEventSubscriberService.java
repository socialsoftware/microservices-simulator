package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;

@Component
@Profile("stream")
public class TournamentEventSubscriberService extends EventSubscriberService {

    public TournamentEventSubscriberService(EventRepository eventRepository, MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Map<String, Class<? extends Event>> getSubscribedEvents() {
        return Map.of(
            UpdateTopicEvent.class.getSimpleName(), UpdateTopicEvent.class,
            DeleteTopicEvent.class.getSimpleName(), DeleteTopicEvent.class,
            UpdateStudentNameEvent.class.getSimpleName(), UpdateStudentNameEvent.class,
            DisenrollStudentFromCourseExecutionEvent.class.getSimpleName(), DisenrollStudentFromCourseExecutionEvent.class
        );
    }

    @Bean
    public Consumer<Message<String>> tournamentEventSubscriber() {
        return this::processEvent;
    }
}
