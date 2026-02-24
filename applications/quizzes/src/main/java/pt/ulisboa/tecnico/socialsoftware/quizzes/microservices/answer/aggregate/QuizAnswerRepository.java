package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    @Query(value = "select a1.aggregateId from QuizAnswer a1 where a1.quiz.quizAggregateId = :quizAggregateId AND a1.student.studentAggregateId = :studentAggregateId ")
    Optional<Integer> findQuizAnswerIdByQuizIdAndUserIdForTCC(Integer quizAggregateId, Integer studentAggregateId);

    @Query(value = "select a1.aggregateId from QuizAnswer a1 where a1.quiz.quizAggregateId = :quizAggregateId AND a1.student.studentAggregateId = :studentAggregateId AND a1.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findQuizAnswerIdByQuizIdAndUserIdForSaga(Integer quizAggregateId, Integer studentAggregateId);

    Optional<QuizAnswer> findTopByOrderByVersionDesc();

    default Optional<QuizAnswer> findLatestQuizAnswer() {
        return findTopByOrderByVersionDesc();
    }
}
