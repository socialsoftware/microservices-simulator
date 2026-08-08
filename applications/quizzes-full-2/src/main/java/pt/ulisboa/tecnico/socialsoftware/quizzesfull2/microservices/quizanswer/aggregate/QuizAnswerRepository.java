package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Integer> {
    @Query("select qa from QuizAnswer qa " +
            "where qa.state = 'ACTIVE' " +
            "and qa.student.userAggregateId = :userAggregateId " +
            "and qa.quiz.quizAggregateId = :quizAggregateId " +
            "and qa.version = (select max(qa2.version) from QuizAnswer qa2 where qa2.aggregateId = qa.aggregateId)")
    List<QuizAnswer> findAllLatestActiveByStudentAndQuiz(@Param("userAggregateId") Integer userAggregateId,
                                                         @Param("quizAggregateId") Integer quizAggregateId);

    @Query(value = "select a1 from QuizAnswer a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<QuizAnswer> findLastAggregateVersion(Integer aggregateId);

    Optional<QuizAnswer> findTopByOrderByVersionDesc();
}
