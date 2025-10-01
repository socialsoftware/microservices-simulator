package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class CourseDto implements Serializable {
    
    private Integer id;
    private String name;
    private String acronym;
    private String courseType;
    
    public CourseDto() {
        // Default constructor
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