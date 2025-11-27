package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventPublisherService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriptionConfig;

@Component
@Profile("stream")
public class QuestionEventPublisherService extends EventPublisherService {

    public QuestionEventPublisherService(EventRepository eventRepository, StreamBridge streamBridge,
            MessagingObjectMapperProvider mapperProvider,
            EventSubscriptionConfig eventSubscriptionConfig) {
        super(eventRepository, streamBridge, mapperProvider, eventSubscriptionConfig, "question");
    }

    @Override
    protected String getAggregatePackageName() {
        return "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question";
    }
}
