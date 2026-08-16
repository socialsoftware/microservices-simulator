package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class TopicCustomRepositorySagas implements TopicCustomRepository {
    @Autowired
    private TopicRepository topicRepository;

    @Override
    public Set<Integer> findTopicIdsByCourse(Integer courseAggregateId) {
        return topicRepository.findAllLatestActiveByCourse(courseAggregateId).stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }
}
