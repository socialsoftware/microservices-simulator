package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import java.util.Set;

public interface CourseCustomRepository {
    Set<Integer> findCourseIdsByName(String courseName);
}