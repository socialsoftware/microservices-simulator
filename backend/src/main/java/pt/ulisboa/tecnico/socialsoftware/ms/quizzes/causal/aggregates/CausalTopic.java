package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicCourse;

@Entity
public class CausalTopic extends Topic implements CausalAggregate {
    public CausalTopic() {
        super();
    }

    public CausalTopic(Integer aggregateId, String name, TopicCourse topicCourse) {
        super(aggregateId, name, topicCourse);
    }

    public CausalTopic(CausalTopic other) {
        super(other);
    }
}
