package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository;


@Service
@Profile("sagas")
public class TopicCustomRepositorySagas implements TopicCustomRepository {

    @Autowired
    private TopicRepository topicRepository;
}
