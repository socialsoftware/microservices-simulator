package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
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

    public TopicService(TopicCustomRepository topicCustomRepository,
                        TopicFactory topicFactory,
                        UnitOfWorkService unitOfWorkService) {
        this.topicCustomRepository = topicCustomRepository;
        this.topicFactory = topicFactory;
        this.unitOfWorkService = unitOfWorkService;
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
}
