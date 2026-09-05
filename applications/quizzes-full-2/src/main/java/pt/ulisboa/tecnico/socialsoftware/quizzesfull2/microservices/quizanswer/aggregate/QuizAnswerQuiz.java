package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_answer_quizzes")
public class QuizAnswerQuiz {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer quizAggregateId;
    private Long quizVersion;
    @OneToOne
    private QuizAnswer quizAnswer;

    public QuizAnswerQuiz() {
    }

    public QuizAnswerQuiz(Integer quizAggregateId, Long quizVersion) {
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
    }

    public QuizAnswerQuiz(QuizAnswerQuiz other) {
        this.quizAggregateId = other.getQuizAggregateId();
        this.quizVersion = other.getQuizVersion();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Long getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Long quizVersion) {
        this.quizVersion = quizVersion;
    }

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
