package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;

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

    @Override
    public CourseExecutionDto createCourseExecutionDto(CourseExecution courseExecution) {
        return new CourseExecutionDto(courseExecution);
    }
}
