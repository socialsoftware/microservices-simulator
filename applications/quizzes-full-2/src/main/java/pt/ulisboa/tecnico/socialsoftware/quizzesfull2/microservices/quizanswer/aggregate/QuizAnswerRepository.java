package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends AggregateRepository {
    @Query("select qa from QuizAnswer qa " +
            "where qa.state = 'ACTIVE' " +
            "and qa.student.userAggregateId = :userAggregateId " +
            "and qa.quiz.quizAggregateId = :quizAggregateId " +
            "and qa.version = (select max(qa2.version) from QuizAnswer qa2 where qa2.aggregateId = qa.aggregateId)")
    List<QuizAnswer> findAllLatestActiveByStudentAndQuiz(@Param("userAggregateId") Integer userAggregateId,
                                                         @Param("quizAggregateId") Integer quizAggregateId);
}
