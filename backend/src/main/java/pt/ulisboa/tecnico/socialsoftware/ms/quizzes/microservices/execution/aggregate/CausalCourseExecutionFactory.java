package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalCourseExecution;

@Service
@Profile("tcc")
public class CausalCourseExecutionFactory implements CourseExecutionFactory {
    @Override
    public CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        return new CausalCourseExecution(aggregateId, courseExecutionDto, courseExecutionCourse);
    }

    @Override
    public CourseExecution createCourseExecutionFromExisting(CourseExecution existingAnswer) {
        return new CausalCourseExecution((CausalCourseExecution) existingAnswer);
    }
}
