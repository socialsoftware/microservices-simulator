package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.TopicEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicRepository;

public abstract class TopicEventHandler extends EventHandler {
    private TopicRepository topicRepository;
    protected TopicEventProcessing topicEventProcessing;

    public TopicEventHandler(TopicRepository topicRepository, TopicEventProcessing topicEventProcessing) {
        this.topicRepository = topicRepository;
        this.topicEventProcessing = topicEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return topicRepository.findAll().stream().map(Topic::getAggregateId).collect(Collectors.toSet());
    }

}
