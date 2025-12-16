package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Integer> {
    @Query(value = "select c1.id from Course c1 where c1.name = :courseName AND c1.state = 'ACTIVE'")
    Optional<Integer> findCourseIdByNameForTCC(String courseName);

    @Query(value = "select c1.id from Course c1 where c1.name = :courseName AND c1.state = 'ACTIVE' AND c1.sagaState = 'NOT_IN_SAGA'")
    Optional<Integer> findCourseIdByNameForSaga(String courseName);

    @Query(value = "select c1 from Course c1 where c1.version = (select max(c2.version) from Course c2)")
    Optional<Course> findLatestCourse();
}
