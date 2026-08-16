package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface CourseRepository extends JpaRepository<Course, Integer> {}
