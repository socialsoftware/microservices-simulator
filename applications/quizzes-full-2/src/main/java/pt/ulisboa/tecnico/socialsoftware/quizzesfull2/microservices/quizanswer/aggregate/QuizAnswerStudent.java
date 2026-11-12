package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_answer_students")
public class QuizAnswerStudent {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userAggregateId;
    private String userName;
    private Long userVersion;
    @OneToOne
    private QuizAnswer quizAnswer;

    public QuizAnswerStudent() {
    }

    public QuizAnswerStudent(Integer userAggregateId, String userName, Long userVersion) {
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userVersion = userVersion;
    }

    public QuizAnswerStudent(QuizAnswerStudent other) {
        this.userAggregateId = other.getUserAggregateId();
        this.userName = other.getUserName();
        this.userVersion = other.getUserVersion();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
