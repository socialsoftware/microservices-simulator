package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public class QuizAnswerStudent {
    @Id
    @GeneratedValue
    private Integer studentAggregateId;
    private String studentName;
    private String studentUsername;
    private String studentEmail;
    @OneToOne
    private Answer answer; 

    public QuizAnswerStudent() {
    }

    public QuizAnswerStudent(AnswerDto answerDto) {
        setStudentName(answerDto.getStudentName());
        setStudentUsername(answerDto.getStudentUsername());
        setStudentEmail(answerDto.getStudentEmail());
    }

    public QuizAnswerStudent(QuizAnswerStudent other) {
        setStudentName(other.getStudentName());
        setStudentUsername(other.getStudentUsername());
        setStudentEmail(other.getStudentEmail());
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

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


}