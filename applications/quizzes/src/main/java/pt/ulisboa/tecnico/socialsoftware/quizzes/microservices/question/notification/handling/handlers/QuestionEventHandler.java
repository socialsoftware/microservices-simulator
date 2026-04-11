package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing.QuestionEventProcessing;

import java.util.HashSet;
import java.util.Set;

public abstract class QuestionEventHandler extends EventHandler {
    private final QuestionRepository questionRepository;
    protected QuestionEventProcessing questionEventProcessing;

    public QuestionEventHandler(QuestionRepository questionRepository, QuestionEventProcessing questionEventProcessing) {
        this.questionRepository = questionRepository;
        this.questionEventProcessing = questionEventProcessing;
    }

    @Override
    public Set<Integer> getAggregateIds() {
        return questionRepository.findAllAggregateIds();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
        return questionRepository.findLastAggregateVersion(subscriberAggregateId)
                .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
                .orElse(new HashSet<>());
    }

}
