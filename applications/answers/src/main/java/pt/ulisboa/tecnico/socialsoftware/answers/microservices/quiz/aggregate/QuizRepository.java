package com.generated.microservices.answers.microservices.quiz.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
        @Query(value = "select quiz.id from Quiz quiz where quiz.title = :title AND quiz.state = 'ACTIVE' AND quiz.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findQuizIdByTitleForSaga(String title);

    @Query(value = "select quiz.id from Quiz quiz where quiz.description = :description AND quiz.state = 'ACTIVE' AND quiz.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findQuizIdByDescriptionForSaga(String description);

    @Query(value = "select quiz.id from Quiz quiz where quiz.quizType = :quizType AND quiz.state = 'ACTIVE' AND quiz.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findQuizIdByQuizTypeForSaga(String quizType);


    }