package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerRepository;

import java.util.Optional;

@Service
@Profile("sagas")
public class QuizAnswerCustomRepositorySagas implements QuizAnswerCustomRepository {

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Override
    public Optional<QuizAnswer> findByQuizAggregateIdAndUserAggregateId(Integer quizAggregateId, Integer userAggregateId) {
        return quizAnswerRepository.findAll().stream()
                .filter(qa -> qa.getState() != Aggregate.AggregateState.DELETED)
                .filter(qa -> quizAggregateId.equals(qa.getQuizAggregateId()))
                .filter(qa -> userAggregateId.equals(qa.getUserAggregateId()))
                .findFirst();
    }
}
