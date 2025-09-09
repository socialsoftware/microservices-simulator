package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Embeddable
public class CourseExecutionStudent {
    private Integer studentAggregateId;
    private String studentName;
    private String studentUsername;
    private String studentEmail;
    private LocalDateTime enrollmentDate; 

    public CourseExecutionStudent(Integer studentAggregateId, String studentName, String studentUsername, String studentEmail, LocalDateTime enrollmentDate) {
        this.studentAggregateId = studentAggregateId;
        this.studentName = studentName;
        this.studentUsername = studentUsername;
        this.studentEmail = studentEmail;
        this.enrollmentDate = enrollmentDate;
    }

    public CourseExecutionStudent(CourseExecutionStudent other) {
        // Copy constructor
    }


    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentUsername() {
        return studentUsername;
    }

    public void setStudentUsername(String studentUsername) {
        this.studentUsername = studentUsername;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }


}