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
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicUpdatedEvent;
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

    public TopicDto getTopicById(Integer id, UnitOfWork unitOfWork) {
        try {
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return topicFactory.createTopicDto(topic);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving topic: " + e.getMessage());
        }
    }

    public List<TopicDto> getAllTopics(UnitOfWork unitOfWork) {
        try {
            // First collect aggregateIds, then load each aggregate through UnitOfWork
            Set<Integer> aggregateIds = topicRepository.findAll().stream()
                .map(Topic::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(topicFactory::createTopicDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all topics: " + e.getMessage());
        }
    }

    public TopicDto updateTopic(TopicDto topicDto, UnitOfWork unitOfWork) {
        try {
            Integer id = topicDto.getAggregateId();
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (topicDto.getName() != null) {
                topic.setName(topicDto.getName());
            }

            unitOfWorkService.registerChanged(topic, unitOfWork);
            return topicFactory.createTopicDto(topic);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating topic: " + e.getMessage());
        }
    }

    public void deleteTopic(Integer id, UnitOfWork unitOfWork) {
        try {
            Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            topic.remove();
            unitOfWorkService.registerChanged(topic, unitOfWork);
            unitOfWorkService.registerEvent(new TopicDeletedEvent(topic.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting topic: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented
}