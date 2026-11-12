package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TopicService {
    private final TopicCustomRepository topicCustomRepository;
    private final TopicFactory topicFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public TopicService(TopicCustomRepository topicCustomRepository,
                        TopicFactory topicFactory,
                        UnitOfWorkService unitOfWorkService,
                        AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.topicCustomRepository = topicCustomRepository;
        this.topicFactory = topicFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto getTopicById(Integer topicAggregateId, UnitOfWork unitOfWork) {
        return topicFactory.createTopicDto(
                (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TopicDto> getTopicsByCourse(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return topicCustomRepository.findTopicIdsByCourse(courseAggregateId).stream()
                .map(topicAggregateId -> topicFactory.createTopicDto(
                        (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TopicDto createTopic(TopicDto topicDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Topic topic = topicFactory.createTopic(aggregateId, topicDto.getName(),
                topicDto.getCourseAggregateId());

        unitOfWorkService.registerChanged(topic, unitOfWork);
        return topicFactory.createTopicDto(topic);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTopic(Integer topicAggregateId, String name, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
        Topic newTopic = topicFactory.createTopicCopy(oldTopic);

        newTopic.setName(name);

        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(
                new UpdateTopicEvent(newTopic.getAggregateId(), newTopic.getName()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTopic(Integer topicAggregateId, UnitOfWork unitOfWork) {
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(topicAggregateId, unitOfWork);
        Topic newTopic = topicFactory.createTopicCopy(oldTopic);

        newTopic.remove();

        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteTopicEvent(newTopic.getAggregateId()), unitOfWork);
    }
}
