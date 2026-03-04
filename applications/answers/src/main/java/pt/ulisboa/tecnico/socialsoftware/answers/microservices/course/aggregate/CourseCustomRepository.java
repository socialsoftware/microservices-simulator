package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.util.Optional;

public interface CourseCustomRepository {
    Optional<Integer> findCourseIdByName(String courseName);
}