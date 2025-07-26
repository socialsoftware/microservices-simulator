package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicFactory;

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

    @Override
    public TopicDto createTopicDto(Topic topic) {
        return new TopicDto(topic);
    }
}
