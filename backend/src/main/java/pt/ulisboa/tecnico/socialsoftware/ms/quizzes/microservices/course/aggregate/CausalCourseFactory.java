package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Service
@Profile("tcc")
public class CausalCourseFactory implements CourseFactory {
    @Override
    public Course createCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        return new CausalCourse(aggregateId, courseExecutionDto);
    }
}
