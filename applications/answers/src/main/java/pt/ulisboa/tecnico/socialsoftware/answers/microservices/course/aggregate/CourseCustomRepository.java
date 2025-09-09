package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface CourseCustomRepository {
    Optional<Integer> findCourseIdByName(String courseName);
}