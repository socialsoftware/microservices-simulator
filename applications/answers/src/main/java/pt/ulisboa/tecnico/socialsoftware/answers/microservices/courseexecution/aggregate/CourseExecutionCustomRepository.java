package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface CourseExecutionCustomRepository {
    Optional<Integer> findCourseExecutionIdByCourseIdAndAcademicTerm(Integer courseId, String academicTerm);
}