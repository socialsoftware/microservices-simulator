package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class CourseDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private Integer id;
    private String name;
    private String acronym;
    private String courseType;
    
    public CourseDto() {
    }
    
    public CourseDto(Integer aggregateId, Integer version, AggregateState state, Integer id, String name, String acronym, String courseType) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setId(id);
        setName(name);
        setAcronym(acronym);
        setCourseType(courseType);
    }
    
    public Integer getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }
    
    public void setState(AggregateState state) {
        this.state = state;
    }

    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }
    
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getCourseType() {
        return courseType;
    }
    
    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }
}