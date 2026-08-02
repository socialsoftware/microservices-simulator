package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate;

import java.util.Set;

public interface CourseCustomRepository {
    Set<Integer> findAllCourseIds();
}
