package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class CourseExecutionCourse {
    private Integer courseAggregateId;
    private String courseName;
    private String courseAcronym;
    private String courseType; 

    public CourseExecutionCourse(Integer courseAggregateId, String courseName, String courseAcronym, String courseType) {
        this.courseAggregateId = courseAggregateId;
        this.courseName = courseName;
        this.courseAcronym = courseAcronym;
        this.courseType = courseType;
    }

    public CourseExecutionCourse(CourseExecutionCourse other) {
        // Copy constructor
    }


    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseAcronym() {
        return courseAcronym;
    }

    public void setCourseAcronym(String courseAcronym) {
        this.courseAcronym = courseAcronym;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }


}