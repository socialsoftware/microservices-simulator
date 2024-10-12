package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
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

    @Override
    public Set<String> getMutableFields() {
        return new HashSet<>();
    }

    @Override
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        return null;
    }
}
