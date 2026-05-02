package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private TopicFactory topicFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final TopicCustomRepository topicRepository;

    public TopicService(UnitOfWorkService unitOfWorkService, TopicCustomRepository topicRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.topicRepository = topicRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto getTopicById(Integer topicAggregateId, UnitOfWork unitOfWork) {
        return topicFactory.createTopicDto(
                (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto createTopic(TopicDto topicDto, TopicCourse course, UnitOfWork unitOfWork) {
        Topic topic = topicFactory.createTopic(aggregateIdGeneratorService.getNewAggregateId(),
                topicDto.getName(), course);
        unitOfWorkService.registerChanged(topic, unitOfWork);
        return topicFactory.createTopicDto(topic);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTopic(TopicDto topicDto, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.getAggregateId(), unitOfWork);
        Topic newTopic = topicFactory.createTopicCopy(oldTopic);
        newTopic.setName(topicDto.getName());
        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(new UpdateTopicEvent(newTopic.getAggregateId(), newTopic.getName()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTopic(Integer topicAggregateId, UnitOfWork unitOfWork) {
        Topic topic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
        topic.remove();
        unitOfWorkService.registerChanged(topic, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteTopicEvent(topic.getAggregateId()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TopicDto> getTopicsByCourseId(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return topicRepository.findTopicIdsByCourseId(courseAggregateId).stream()
                .map(aggregateId -> (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork))
                .map(t -> topicFactory.createTopicDto(t))
                .collect(Collectors.toList());
    }
}
