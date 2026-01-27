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
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTopicRequestDto;


@Service
@Transactional
public class TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicFactory topicFactory;

    public TopicService() {}

    public TopicDto createTopic(CreateTopicRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TopicDto topicDto = new TopicDto();
            topicDto.setName(createRequest.getName());
            if (createRequest.getCourse() != null) {
                TopicCourseDto courseDto = new TopicCourseDto();
                courseDto.setAggregateId(createRequest.getCourse().getAggregateId());
                courseDto.setVersion(createRequest.getCourse().getVersion());
                courseDto.setState(createRequest.getCourse().getState());
                topicDto.setCourse(courseDto);
            }
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Topic topic = topicFactory.createTopic(aggregateId, topicDto);
            unitOfWorkService.registerChanged(topic, unitOfWork);
            return topicFactory.createTopicDto(topic);
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