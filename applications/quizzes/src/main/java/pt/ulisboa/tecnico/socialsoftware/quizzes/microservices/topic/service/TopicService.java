package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final TopicRepository topicRepository;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private TopicFactory topicFactory;
    
    public TopicService(UnitOfWorkService unitOfWorkService, TopicRepository topicRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.topicRepository = topicRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto getTopicById(Integer topicAggregateId, UnitOfWork unitOfWork) {
        return topicFactory.createTopicDto((Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto createTopic(TopicDto topicDto, TopicCourse course, UnitOfWork unitOfWork) { //TODO check this
        Topic topic = topicFactory.createTopic(aggregateIdGeneratorService.getNewAggregateId(),
                topicDto.getName(), course);
        unitOfWorkService.registerChanged(topic, unitOfWork);
        return topicFactory.createTopicDto(topic);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TopicDto> findTopicsByCourseId(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return topicRepository.findAll().stream()
                .filter(t -> courseAggregateId == t.getTopicCourse().getCourseAggregateId())
                .map(Topic::getAggregateId)
                .distinct()
                .map(aggregateId -> (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork))
                .map(TopicDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTopic(TopicDto topicDto, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto.getAggregateId(), unitOfWork);
        Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
        newTopic.setName(topicDto.getName());
        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(new UpdateTopicEvent(newTopic.getAggregateId(), newTopic.getName()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTopic(Integer topicAggregateId, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
        Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
        newTopic.remove();
        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteTopicEvent(newTopic.getAggregateId()), unitOfWork);
    }
}
