package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class QuizAnswerCourseExecution {
    private Integer courseExecutionAggregateId;
    private String courseExecutionName;
    private String courseExecutionAcronym;
    private String courseExecutionAcademicTerm; 

    public QuizAnswerCourseExecution(Integer courseExecutionAggregateId, String courseExecutionName, String courseExecutionAcronym, String courseExecutionAcademicTerm) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
        this.courseExecutionName = courseExecutionName;
        this.courseExecutionAcronym = courseExecutionAcronym;
        this.courseExecutionAcademicTerm = courseExecutionAcademicTerm;
    }

    public QuizAnswerCourseExecution(QuizAnswerCourseExecution other) {
        // Copy constructor
    }


    public Integer getCourseExecutionAggregateId() {
        return courseExecutionAggregateId;
    }

    public void setCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
        this.courseExecutionAggregateId = courseExecutionAggregateId;
    }

    public String getCourseExecutionName() {
        return courseExecutionName;
    }

    public void setCourseExecutionName(String courseExecutionName) {
        this.courseExecutionName = courseExecutionName;
    }

    public String getCourseExecutionAcronym() {
        return courseExecutionAcronym;
    }

    public void setCourseExecutionAcronym(String courseExecutionAcronym) {
        this.courseExecutionAcronym = courseExecutionAcronym;
    }

    public String getCourseExecutionAcademicTerm() {
        return courseExecutionAcademicTerm;
    }

    public void setCourseExecutionAcademicTerm(String courseExecutionAcademicTerm) {
        this.courseExecutionAcademicTerm = courseExecutionAcademicTerm;
    }


}