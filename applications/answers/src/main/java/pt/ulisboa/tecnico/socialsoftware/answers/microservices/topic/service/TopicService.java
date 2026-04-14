package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicCourseDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;


@Service
@Transactional(noRollbackFor = AnswersException.class)
public class TopicService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicFactory topicFactory;

    @Autowired
    private TopicServiceExtension extension;

    public TopicService() {}

    public TopicDto createTopic(CreateTopicRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TopicDto topicDto = new TopicDto();
            topicDto.setName(createRequest.getName());
            if (createRequest.getCourse() != null) {
                Course refSource = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getCourse().getAggregateId(), unitOfWork);
                CourseDto refSourceDto = new CourseDto(refSource);
                TopicCourseDto courseDto = new TopicCourseDto();
                courseDto.setAggregateId(refSourceDto.getAggregateId());
                courseDto.setVersion(refSourceDto.getVersion());
                courseDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);

                topicDto.setCourse(courseDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Topic topic = topicFactory.createTopic(aggregateId, topicDto);
            unitOfWorkService.registerChanged(topic, unitOfWork);
            return topicFactory.createTopicDto(topic);
        } catch (AnswersException e) {
            throw e;
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
            Set<Integer> aggregateIds = topicRepository.findAll().stream()
                .map(Topic::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(topicFactory::createTopicDto)
                .collect(Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving topic: " + e.getMessage());
        }
    }

    public TopicDto updateTopic(TopicDto topicDto, UnitOfWork unitOfWork) {
        try {
            Integer id = topicDto.getAggregateId();
            Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
            if (topicDto.getName() != null) {
                newTopic.setName(topicDto.getName());
            }

            unitOfWorkService.registerChanged(newTopic, unitOfWork);            TopicUpdatedEvent event = new TopicUpdatedEvent(newTopic.getAggregateId(), newTopic.getName());
            event.setPublisherAggregateVersion(newTopic.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return topicFactory.createTopicDto(newTopic);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating topic: " + e.getMessage());
        }
    }

    public void deleteTopic(Integer id, UnitOfWork unitOfWork) {
        try {
            Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
            newTopic.remove();
            unitOfWorkService.registerChanged(newTopic, unitOfWork);            unitOfWorkService.registerEvent(new TopicDeletedEvent(newTopic.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting topic: " + e.getMessage());
        }
    }








}