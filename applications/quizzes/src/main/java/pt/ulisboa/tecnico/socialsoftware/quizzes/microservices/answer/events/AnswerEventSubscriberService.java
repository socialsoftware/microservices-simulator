package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;

import java.util.function.Consumer;

@Component
@Profile("!local")
public class AnswerEventSubscriberService extends EventSubscriberService {

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    public AnswerEventSubscriberService(EventRepository eventRepository, MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Aggregate getLatestAggregate() {
        return quizAnswerRepository.findLatestQuizAnswer().orElse(null);
    }

    @Override
    public String getEventPackage() {
        return "pt.ulisboa.tecnico.socialsoftware.quizzes.events";
    }

    @Bean
    public Consumer<Message<String>> answerEventSubscriber() {
        return this::processEvent;
    }
}
