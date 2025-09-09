package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class QuizAnswerStudent {
    private Integer studentAggregateId;
    private String studentName;
    private String studentUsername;
    private String studentEmail; 

    public QuizAnswerStudent(Integer studentAggregateId, String studentName, String studentUsername, String studentEmail) {
        this.studentAggregateId = studentAggregateId;
        this.studentName = studentName;
        this.studentUsername = studentUsername;
        this.studentEmail = studentEmail;
    }

    public QuizAnswerStudent(QuizAnswerStudent other) {
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


}