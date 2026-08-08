package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import java.util.Optional;

public interface QuizAnswerCustomRepository {
    Optional<Integer> findQuizAnswerIdByStudentAndQuiz(Integer userAggregateId, Integer quizAggregateId);
}
