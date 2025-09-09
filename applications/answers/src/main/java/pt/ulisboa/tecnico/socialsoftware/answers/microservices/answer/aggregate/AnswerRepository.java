package com.generated.microservices.answers.microservices.answer.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    
    }