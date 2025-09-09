package com.generated.microservices.answers.microservices.courseexecution.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface CourseExecutionRepository extends JpaRepository<CourseExecution, Integer> {
        @Query(value = "select courseexecution.id from CourseExecution courseexecution where courseexecution.name = :name AND courseexecution.state = 'ACTIVE' AND courseexecution.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseExecutionIdByNameForSaga(String name);

    @Query(value = "select courseexecution.id from CourseExecution courseexecution where courseexecution.acronym = :acronym AND courseexecution.state = 'ACTIVE' AND courseexecution.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseExecutionIdByAcronymForSaga(String acronym);

    @Query(value = "select courseexecution.id from CourseExecution courseexecution where courseexecution.academicTerm = :academicTerm AND courseexecution.state = 'ACTIVE' AND courseexecution.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseExecutionIdByAcademicTermForSaga(String academicTerm);


    }