package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicFactory topicFactory;

    public TopicService() {}

    // CRUD Operations
    public TopicDto createTopic(String name, TopicCourse course) {
        try {
            Topic topic = new Topic(name, course);
            topic = topicRepository.save(topic);
            return new TopicDto(topic);
        } catch (Exception e) {
            throw new AnswersException("Error creating topic: " + e.getMessage());
        }
    }

    public TopicDto getTopicById(Integer id) {
        try {
            Topic topic = (Topic) topicRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Topic not found with id: " + id));
            return new TopicDto(topic);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving topic: " + e.getMessage());
        }
    }

    public List<TopicDto> getAllTopics() {
        try {
            return topicRepository.findAll().stream()
                .map(entity -> new TopicDto((Topic) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all topics: " + e.getMessage());
        }
    }

    public TopicDto updateTopic(TopicDto topicDto) {
        try {
            Integer id = topicDto.getAggregateId();
            Topic topic = (Topic) topicRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Topic not found with id: " + id));
            
                        if (topicDto.getName() != null) {
                topic.setName(topicDto.getName());
            }
            if (topicDto.getCourse() != null) {
                topic.setCourse(topicDto.getCourse());
            }
            
            topic = topicRepository.save(topic);
            return new TopicDto(topic);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating topic: " + e.getMessage());
        }
    }

    public void deleteTopic(Integer id) {
        try {
            if (!topicRepository.existsById(id)) {
                throw new AnswersException("Topic not found with id: " + id);
            }
            topicRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting topic: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented

    // Event Processing Methods
    private void publishTopicCreatedEvent(Topic topic) {
        try {
            // TODO: Implement event publishing for TopicCreated
            // eventPublisher.publishEvent(new TopicCreatedEvent(topic));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish TopicCreatedEvent", e);
        }
    }

    private void publishTopicUpdatedEvent(Topic topic) {
        try {
            // TODO: Implement event publishing for TopicUpdated
            // eventPublisher.publishEvent(new TopicUpdatedEvent(topic));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish TopicUpdatedEvent", e);
        }
    }

    private void publishTopicDeletedEvent(Long topicId) {
        try {
            // TODO: Implement event publishing for TopicDeleted
            // eventPublisher.publishEvent(new TopicDeletedEvent(topicId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish TopicDeletedEvent", e);
        }
    }
}