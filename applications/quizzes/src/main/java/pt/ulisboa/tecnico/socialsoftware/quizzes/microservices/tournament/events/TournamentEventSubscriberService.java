package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;

import java.util.function.Consumer;

@Configuration
@Profile("stream")
public class TournamentEventSubscriberService extends EventSubscriberService {

    public TournamentEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider, "tournament");
    }

    @Bean
    public Consumer<Message<String>> tournamentEventChannel() {
        return this;
    }

    @Override
    protected Class<? extends Event> getEventClass(String eventType) {
        return switch (eventType) {
            case "UpdateTopicEvent" -> UpdateTopicEvent.class;
            case "DeleteTopicEvent" -> DeleteTopicEvent.class;
            case "UpdateStudentNameEvent" -> UpdateStudentNameEvent.class;
            case "DisenrollStudentFromCourseExecutionEvent" -> DisenrollStudentFromCourseExecutionEvent.class;
            case "DeleteCourseExecutionEvent" -> DeleteCourseExecutionEvent.class;
            case "InvalidateQuizEvent" -> InvalidateQuizEvent.class;
            case "AnonymizeStudentEvent" -> AnonymizeStudentEvent.class;
            case "QuizAnswerQuestionAnswerEvent" -> QuizAnswerQuestionAnswerEvent.class;
            default -> null;
        };
    }
}
