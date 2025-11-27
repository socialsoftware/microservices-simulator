package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;

import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class AnswerEventSubscriberService extends EventSubscriberService {

    public AnswerEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider, "answer");
    }

    @Bean
    public Consumer<Message<String>> answerEventChannel() {
        return this;
    }

    @Override
    protected Class<? extends Event> getEventClass(String eventType) {
        return switch (eventType) {
            case "UpdateStudentNameEvent" -> UpdateStudentNameEvent.class;
            case "DisenrollStudentFromCourseExecutionEvent" -> DisenrollStudentFromCourseExecutionEvent.class;
            case "DeleteCourseExecutionEvent" -> DeleteCourseExecutionEvent.class;
            case "InvalidateQuizEvent" -> InvalidateQuizEvent.class;
            case "AnonymizeStudentEvent" -> AnonymizeStudentEvent.class;
            default -> null;
        };
    }
}
