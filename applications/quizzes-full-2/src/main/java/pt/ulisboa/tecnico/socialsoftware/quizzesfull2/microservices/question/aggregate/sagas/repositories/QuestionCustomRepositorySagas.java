package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class QuestionCustomRepositorySagas implements QuestionCustomRepository {
    @Autowired
    private QuestionRepository questionRepository;

    @Override
    public Set<Integer> findQuestionIdsByCourse(Integer courseAggregateId) {
        return questionRepository.findAllLatestActiveByCourse(courseAggregateId).stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }
}
