package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.repositories;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerRepository;

@Service
@Profile("tcc")
public class QuizAnswerCustomRepositoryTCC implements QuizAnswerCustomRepository {

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Override
    public Optional<Integer> findQuizAnswerIdByQuizIdAndUserId(Integer quizAggregateId, Integer studentAggregateId) {
        return quizAnswerRepository.findQuizAnswerIdByQuizIdAndUserIdForTCC(quizAggregateId, studentAggregateId);
    }
}