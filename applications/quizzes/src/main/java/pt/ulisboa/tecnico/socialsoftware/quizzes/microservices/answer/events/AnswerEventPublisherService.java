package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventPublisherService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriptionConfig;

@Component
@Profile("stream")
public class AnswerEventPublisherService extends EventPublisherService {

    public AnswerEventPublisherService(EventRepository eventRepository, StreamBridge streamBridge,
            MessagingObjectMapperProvider mapperProvider,
            EventSubscriptionConfig eventSubscriptionConfig) {
        super(eventRepository, streamBridge, mapperProvider, eventSubscriptionConfig, "answer");
    }
}
