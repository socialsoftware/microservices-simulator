package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Service
public class TopicFactory {

    public Topic createTopic(Integer aggregateId, TopicDto topicDto) {
        // Factory method implementation - create root entity directly
        // Extract properties from DTO and create the root entity
        return new Topic(
            topicDto.getName(),
            topicDto.getCourse(),
            topicDto.getCreationDate()
        );
    }

    public Topic createTopicFromExisting(Topic existingTopic) {
        // Create a copy of the existing aggregate
        if (existingTopic instanceof Topic) {
            return new Topic((Topic) existingTopic);
        }
        throw new IllegalArgumentException("Unknown aggregate type");
    }

    public TopicDto createTopicDto(Topic topic) {
        return new TopicDto((Topic) topic);
    }
}