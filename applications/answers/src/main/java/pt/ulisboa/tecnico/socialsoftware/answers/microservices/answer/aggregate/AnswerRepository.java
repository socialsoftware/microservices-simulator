package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    @Query(value = "select a.aggregateId from Answer a where a.quiz.quizAggregateId = :quizAggregateId AND a.user.userAggregateId = :userAggregateId AND a.sagaState = 'NOT_IN_SAGA' AND a.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findAnswerIdByQuizAggregateIdAndUserAggregateIdForSaga(Integer quizAggregateId, Integer userAggregateId);
}