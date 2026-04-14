package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Integer> {
    @Query(value = "select c1.aggregateId from Course c1 where c1.name = :courseName AND c1.state = 'ACTIVE'")
    Set<Integer> findCourseIdsByName(String courseName);
}