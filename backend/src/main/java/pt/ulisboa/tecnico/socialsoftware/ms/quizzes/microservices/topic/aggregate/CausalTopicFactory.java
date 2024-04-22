package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalTopic;

@Service
@Profile("tcc")
public class CausalTopicFactory implements TopicFactory {

    @Override
    public Topic createTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        return new CausalTopic(aggregateId, name, topicCourse);
    }

    @Override
    public Topic createTopicFromExisting(Topic existingTopic) {
        return new CausalTopic((CausalTopic) existingTopic);
    }
    
}
