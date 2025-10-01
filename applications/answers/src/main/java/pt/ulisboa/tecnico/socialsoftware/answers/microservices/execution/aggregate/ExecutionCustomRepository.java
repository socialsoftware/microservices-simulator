package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface ExecutionCustomRepository {
    Optional<Integer> findExecutionIdByCourseIdAndAcademicTerm(Integer courseId, String academicTerm);
}