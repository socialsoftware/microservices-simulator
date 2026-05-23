package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;

import java.time.LocalDateTime;

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

    @Column
    private LocalDateTime firstAnswerTime;

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
        this.firstAnswerTime = other.getFirstAnswerTime();
    }

    public void linkQuizAnswer(Integer quizAnswerAggregateId, Long quizAnswerVersion) {
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.quizAnswerVersion = quizAnswerVersion;
        if (this.firstAnswerTime == null) {
            this.firstAnswerTime = DateHandler.now();
        }
        this.answered = true;
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

    public LocalDateTime getFirstAnswerTime() { return firstAnswerTime; }
    public void setFirstAnswerTime(LocalDateTime firstAnswerTime) { this.firstAnswerTime = firstAnswerTime; }
}
