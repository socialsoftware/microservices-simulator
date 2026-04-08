package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.causal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.util.HashSet;
import java.util.Set;

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
    @JsonIgnore
    public Set<String> getMutableFields() {
        return Set.of("courseQuestionCount", "courseExecutionCount");
    }

    @Override
    @JsonIgnore
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        CausalCourse prev = (CausalCourse) getPrev();
        CausalCourse committed = (CausalCourse) committedVersion;
        this.setCourseQuestionCount(committed.getCourseQuestionCount() + (this.getCourseQuestionCount() - prev.getCourseQuestionCount()));
        this.setCourseExecutionCount(committed.getCourseExecutionCount() + (this.getCourseExecutionCount() - prev.getCourseExecutionCount()));
        return this;
    }
}
