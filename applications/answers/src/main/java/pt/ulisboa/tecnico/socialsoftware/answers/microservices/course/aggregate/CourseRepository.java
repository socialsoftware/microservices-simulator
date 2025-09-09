package com.generated.microservices.answers.microservices.course.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Integer> {
        @Query(value = "select course.id from Course course where course.name = :name AND course.state = 'ACTIVE' AND course.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseIdByNameForSaga(String name);

    @Query(value = "select course.id from Course course where course.acronym = :acronym AND course.state = 'ACTIVE' AND course.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseIdByAcronymForSaga(String acronym);

    @Query(value = "select course.id from Course course where course.courseType = :courseType AND course.state = 'ACTIVE' AND course.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseIdByCourseTypeForSaga(String courseType);


    }