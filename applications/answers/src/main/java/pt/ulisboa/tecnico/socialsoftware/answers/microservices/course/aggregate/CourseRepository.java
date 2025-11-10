package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Integer> {
    @Query(value = "")
    Optional<Integer> findCourseIdByName(String courseName);
}