package com.generated.microservices.answers.microservices.topic.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TopicRepository extends JpaRepository<Topic, Integer> {
        @Query(value = "select topic.id from Topic topic where topic.name = :name AND topic.state = 'ACTIVE' AND topic.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findTopicIdByNameForSaga(String name);


    }