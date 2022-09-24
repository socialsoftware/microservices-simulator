package pt.ulisboa.tecnico.socialsoftware.blcm.course.domain;

import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.blcm.execution.dto.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.EventualConsistencyDependency;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.AggregateType.COURSE;

@Entity
@Table(name = "courses")
public class Course extends Aggregate {

    @Enumerated(EnumType.STRING)
    private CourseType type;

    @Column
    private String name;

    public Course() {

    }

    public Course(Integer aggregateId, Integer version, CourseExecutionDto courseExecutionDto) {
        super(aggregateId, COURSE);
        setName(courseExecutionDto.getName());
        setType(CourseType.valueOf(courseExecutionDto.getType()));
    }

    public Course(Course other) {
        super(other.getAggregateId(), COURSE);
        setId(null);
        setName(other.getName());
        setType(other.getType());
        setPrev(other);
    }

    @Override
    public boolean verifyInvariants() {
        return true;
    }

    @Override
    public Aggregate merge(Aggregate other) {
        return this;
    }

    @Override
    public Map<Integer, EventualConsistencyDependency> getDependenciesMap() {
        Map deps = new HashMap<>();
        deps.put(getAggregateId(), new EventualConsistencyDependency(getAggregateId(), getAggregateType(), getVersion()));
        return deps;
    }

    public CourseType getType() {
        return type;
    }

    public void setType(CourseType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
