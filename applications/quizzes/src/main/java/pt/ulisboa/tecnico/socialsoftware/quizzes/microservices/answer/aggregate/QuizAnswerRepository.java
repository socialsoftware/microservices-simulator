package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    @Query(value = "select a1.aggregateId from QuizAnswer a1 where a1.quiz.quizAggregateId = :quizAggregateId AND a1.student.studentAggregateId = :studentAggregateId ")
    Optional<Integer> findQuizAnswerIdByQuizIdAndUserIdForTCC(Integer quizAggregateId, Integer studentAggregateId);

    @Query(value = "select a1.aggregateId from QuizAnswer a1 where a1.quiz.quizAggregateId = :quizAggregateId AND a1.student.studentAggregateId = :studentAggregateId AND a1.sagaState = :sagaState")
    Optional<Integer> findQuizAnswerIdByQuizIdAndUserIdForSagaInternal(@Param("quizAggregateId") Integer quizAggregateId, @Param("studentAggregateId") Integer studentAggregateId, @Param("sagaState") SagaAggregate.SagaState sagaState);

    default Optional<Integer> findQuizAnswerIdByQuizIdAndUserIdForSaga(Integer quizAggregateId, Integer studentAggregateId) {
        return findQuizAnswerIdByQuizIdAndUserIdForSagaInternal(quizAggregateId, studentAggregateId, GenericSagaState.NOT_IN_SAGA);
    }

    @Query("select count(a) > 0 from QuizAnswer a where a.quiz.quizAggregateId = :quizAggregateId AND a.student.studentAggregateId = :studentAggregateId AND a.sagaState = :sagaState")
    boolean existsByQuizIdAndStudentIdForSagaInternal(@Param("quizAggregateId") Integer quizAggregateId, @Param("studentAggregateId") Integer studentAggregateId, @Param("sagaState") SagaAggregate.SagaState sagaState);

    default boolean existsByQuizIdAndStudentIdForSaga(Integer quizAggregateId, Integer studentAggregateId) {
        return existsByQuizIdAndStudentIdForSagaInternal(quizAggregateId, studentAggregateId, GenericSagaState.NOT_IN_SAGA);
    }

    @Query("select count(a) > 0 from QuizAnswer a where a.quiz.quizAggregateId = :quizAggregateId AND a.student.studentAggregateId = :studentAggregateId")
    boolean existsByQuizIdAndStudentIdForTCC(@Param("quizAggregateId") Integer quizAggregateId, @Param("studentAggregateId") Integer studentAggregateId);

    Optional<QuizAnswer> findTopByOrderByVersionDesc();

    default Optional<QuizAnswer> findLatestQuizAnswer() {
        return findTopByOrderByVersionDesc();
    }

    @Query("SELECT a.aggregateId FROM QuizAnswer a")
    Set<Integer> findAllAggregateIds();

    @Query(value = "select a1 from QuizAnswer a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<QuizAnswer> findLastAggregateVersion(Integer aggregateId);
}
