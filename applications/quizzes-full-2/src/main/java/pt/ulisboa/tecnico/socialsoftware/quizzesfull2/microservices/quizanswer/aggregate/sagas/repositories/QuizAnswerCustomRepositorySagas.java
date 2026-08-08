package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerRepository;

import java.util.Optional;

@Service
@Profile("sagas")
public class QuizAnswerCustomRepositorySagas implements QuizAnswerCustomRepository {
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Override
    public Optional<Integer> findQuizAnswerIdByStudentAndQuiz(Integer userAggregateId, Integer quizAggregateId) {
        return quizAnswerRepository.findAllLatestActiveByStudentAndQuiz(userAggregateId, quizAggregateId).stream()
                .map(Aggregate::getAggregateId)
                .findFirst();
    }
}
