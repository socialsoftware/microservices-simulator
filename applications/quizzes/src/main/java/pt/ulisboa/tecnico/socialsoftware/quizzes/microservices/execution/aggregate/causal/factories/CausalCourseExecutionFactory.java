package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal.CausalExecution;

@Service
@Profile("tcc")
public class CausalCourseExecutionFactory implements CourseExecutionFactory {
    @Override
    public Execution createCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        return new CausalExecution(aggregateId, courseExecutionDto, courseExecutionCourse);
    }

    @Override
    public Execution createCourseExecutionFromExisting(Execution existingAnswer) {
        return new CausalExecution((CausalExecution) existingAnswer);
    }

    @Override
    public CourseExecutionDto createCourseExecutionDto(Execution execution) {
        return new CourseExecutionDto(execution);
    }
}
