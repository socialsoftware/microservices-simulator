package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;

import java.util.HashSet;
import java.util.Set;

public abstract class QuizEventHandler extends EventHandler {
    private final QuizRepository quizRepository;
    protected QuizEventProcessing quizEventProcessing;

    public QuizEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        this.quizRepository = quizRepository;
        this.quizEventProcessing = quizEventProcessing;
    }

    @Override
    public Set<Integer> getAggregateIds() {
        return quizRepository.findAllAggregateIds();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId, Class<? extends Event> eventClass) {
        return quizRepository.findLastAggregateVersion(subscriberAggregateId)
                .map(aggregate -> aggregate.getEventSubscriptionsByEventType(eventClass.getSimpleName()))
                .orElse(new HashSet<>());
    }

}
