package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class TopicCustomRepositorySagas implements TopicCustomRepository {

    @Autowired
    private TopicRepository topicRepository;

    @Override
    public List<Integer> findTopicIdsByCourseId(Integer courseAggregateId) {
        return topicRepository.findAll().stream()
                .filter(t -> courseAggregateId.equals(t.getTopicCourse().getCourseAggregateId()))
                .map(Topic::getAggregateId)
                .distinct()
                .collect(Collectors.toList());
    }
}
