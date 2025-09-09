package com.generated.microservices.answers.microservices.question.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
        @Query(value = "select question.id from Question question where question.title = :title AND question.state = 'ACTIVE' AND question.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findQuestionIdByTitleForSaga(String title);

    @Query(value = "select question.id from Question question where question.content = :content AND question.state = 'ACTIVE' AND question.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findQuestionIdByContentForSaga(String content);


    }