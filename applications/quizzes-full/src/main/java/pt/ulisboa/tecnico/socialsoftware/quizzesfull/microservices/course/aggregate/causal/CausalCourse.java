package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.causal;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;

import java.util.Set;

@Entity
public class CausalCourse extends Course implements CausalAggregate {

    public CausalCourse() {
        super();
    }

    public CausalCourse(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId, courseDto);
    }

    public CausalCourse(CausalCourse other) {
        super(other);
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of();  // TCC not implemented
    }

    @Override
    public Set<String[]> getIntentions() {
        return Set.of();  // TCC not implemented
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitChangedFields,
                                  Aggregate committedVersion,
                                  Set<String> committedChangedFields) {
        return this;  // TCC not implemented — no merge
    }
}
