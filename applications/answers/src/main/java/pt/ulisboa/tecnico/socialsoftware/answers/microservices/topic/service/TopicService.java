package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicDeletedEvent;

@Service
public class TopicService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final TopicRepository topicRepository;

    @Autowired
    private TopicFactory topicFactory;

    public TopicService(UnitOfWorkService unitOfWorkService, TopicRepository topicRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.topicRepository = topicRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto createTopic(TopicDto topicDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Topic topic = topicFactory.createTopic(aggregateId, topicDto);
        unitOfWorkService.registerChanged(topic, unitOfWork);
        return topicFactory.createTopicDto(topic);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto getTopicById(Integer aggregateId, UnitOfWork unitOfWork) {
        return topicFactory.createTopicDto((Topic) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto updateTopic(Integer aggregateId, TopicDto topicDto, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
        newTopic.setName(topicDto.getName());
        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(new TopicUpdatedEvent(newTopic.getAggregateId(), newTopic.getName()), unitOfWork);
        return topicFactory.createTopicDto(newTopic);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTopic(Integer aggregateId, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
        newTopic.remove();
        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(new TopicDeletedEvent(newTopic.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TopicDto> searchTopics(String name, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = topicRepository.findAll().stream()
                .filter(entity -> {
                    if (name != null) {
                        if (!entity.getName().equals(name)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(Topic::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(topicFactory::createTopicDto)
                .collect(Collectors.toList());
    }

}
