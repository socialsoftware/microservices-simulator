package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class CourseDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Root entity fields
    private String acronym;

    // Fields from CourseDetails
    private Integer id;
    private String name;
    private String courseType;
    
    public CourseDto() {
    }
    
    public CourseDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course course) {
        // Standard aggregate fields
        setAggregateId(course.getAggregateId());
        setVersion(course.getVersion());
        setState(course.getState().toString());

        // Root entity fields
        setAcronym(course.getAcronym());

        // Fields from CourseDetails
        setId(course.getCourseDetails().getId());
        setName(course.getCourseDetails().getCourseName());
        setCourseType(course.getCourseDetails().getCourseType().toString());

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

    public String getState() {
        return state;
    }
    
    public void setState(String state) {
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