package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_participant_quiz_answer")
public class TournamentParticipantQuizAnswer {

    @Id
    @GeneratedValue
    private Integer id;

    private Integer quizAnswerAggregateId;
    private Long quizAnswerVersion;
    private Boolean answered;
    private Integer numberOfAnswered;
    private Integer numberOfCorrect;

    public TournamentParticipantQuizAnswer() {
        this.answered = false;
        this.numberOfAnswered = 0;
        this.numberOfCorrect = 0;
    }

    public TournamentParticipantQuizAnswer(TournamentParticipantQuizAnswer other) {
        this.quizAnswerAggregateId = other.getQuizAnswerAggregateId();
        this.quizAnswerVersion = other.getQuizAnswerVersion();
        this.answered = other.getAnswered();
        this.numberOfAnswered = other.getNumberOfAnswered();
        this.numberOfCorrect = other.getNumberOfCorrect();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getQuizAnswerAggregateId() { return quizAnswerAggregateId; }
    public void setQuizAnswerAggregateId(Integer quizAnswerAggregateId) { this.quizAnswerAggregateId = quizAnswerAggregateId; }

    public Long getQuizAnswerVersion() { return quizAnswerVersion; }
    public void setQuizAnswerVersion(Long quizAnswerVersion) { this.quizAnswerVersion = quizAnswerVersion; }

    public Boolean getAnswered() { return answered; }
    public void setAnswered(Boolean answered) { this.answered = answered; }

    public Integer getNumberOfAnswered() { return numberOfAnswered; }
    public void setNumberOfAnswered(Integer numberOfAnswered) { this.numberOfAnswered = numberOfAnswered; }

    public Integer getNumberOfCorrect() { return numberOfCorrect; }
    public void setNumberOfCorrect(Integer numberOfCorrect) { this.numberOfCorrect = numberOfCorrect; }
}
