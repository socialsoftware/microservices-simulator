package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

public class QuizAnswerDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private LocalDateTime answerDate;
    private boolean completed;
    private Integer studentAggregateId;
    private String studentName;
    private Integer quizAggregateId;
    private String state;

    //private List<QuestionAnswer> questionAnswers;

    public QuizAnswerDto(QuizAnswer quizAnswer) {
        setAggregateId(quizAnswer.getAggregateId());
        setVersion(quizAnswer.getVersion());
        setAnswerDate(quizAnswer.getAnswerDate());
        setCompleted(quizAnswer.isCompleted());
        setStudentAggregateId(quizAnswer.getStudent().getStudentAggregateId());
        setStudentName(quizAnswer.getStudent().getName());
        setQuizAggregateId(quizAnswer.getQuiz().getQuizAggregateId());
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

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
