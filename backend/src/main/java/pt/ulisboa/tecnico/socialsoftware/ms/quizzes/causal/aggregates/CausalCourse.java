package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Entity
public class CausalCourse extends Course implements CausalAggregate {
    public CausalCourse() {
        super();
    }

    public CausalCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        super(aggregateId, courseExecutionDto);
    }

    public CausalCourse(CausalCourse other) {
        super(other);
    }
}
