package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.UpdateQuestionEvent;

import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class QuizEventSubscriberService extends EventSubscriberService {

    public QuizEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider, "quiz");
    }

    @Bean
    public Consumer<Message<String>> quizEventChannel() {
        return this;
    }

    @Override
    protected Class<? extends Event> getEventClass(String eventType) {
        return switch (eventType) {
            case "UpdateQuestionEvent" -> UpdateQuestionEvent.class;
            case "DeleteQuestionEvent" -> DeleteQuestionEvent.class;
            case "DeleteCourseExecutionEvent" -> DeleteCourseExecutionEvent.class;
            default -> null;
        };
    }
}
