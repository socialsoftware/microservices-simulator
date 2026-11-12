package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class QuizCustomRepositorySagas implements QuizCustomRepository {
    @Autowired
    private QuizRepository quizRepository;

    @Override
    public Set<Integer> findQuizIdsByExecution(Integer executionAggregateId) {
        return quizRepository.findAllLatestActiveByExecution(executionAggregateId).stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }
}
