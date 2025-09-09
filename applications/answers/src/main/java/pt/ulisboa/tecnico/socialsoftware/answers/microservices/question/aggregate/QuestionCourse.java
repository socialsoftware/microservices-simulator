package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class QuestionCourse {
    private Integer courseAggregateId;
    private String courseName;
    private String courseAcronym; 

    public QuestionCourse(Integer courseAggregateId, String courseName, String courseAcronym) {
        this.courseAggregateId = courseAggregateId;
        this.courseName = courseName;
        this.courseAcronym = courseAcronym;
    }

    public QuestionCourse(QuestionCourse other) {
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


}