package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;

import java.util.HashSet;
import java.util.Set;

public abstract class QuizAnswerEventHandler extends EventHandler {
    private final QuizAnswerRepository quizAnswerRepository;
    protected QuizAnswerEventProcessing quizAnswerEventProcessing;

    public QuizAnswerEventHandler(QuizAnswerRepository quizAnswerRepository, QuizAnswerEventProcessing quizAnswerEventProcessing) {
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizAnswerEventProcessing = quizAnswerEventProcessing;
    }

    @Override
    public Set<Integer> getAggregateIds() {
        return quizAnswerRepository.findAllAggregateIds();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
        return quizAnswerRepository.findLastAggregateVersion(subscriberAggregateId)
                .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
                .orElse(new HashSet<>());
    }

}
