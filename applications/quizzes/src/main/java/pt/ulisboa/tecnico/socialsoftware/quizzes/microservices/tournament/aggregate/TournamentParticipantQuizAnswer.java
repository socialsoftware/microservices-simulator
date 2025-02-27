package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

@Entity
public class TournamentParticipantQuizAnswer {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAnswerAggregateId;
    private Integer quizAnswerVersion;
    private boolean answered;
    // TODO: to implement using the set of answers
    private Integer numberOfAnswered;
    private Integer numberOfCorrect;
    @OneToOne
    private TournamentParticipant tournamentParticipant;

    public TournamentParticipantQuizAnswer() {
        setQuizAnswerVersion(0);
        setAnswered(false);
        this.numberOfAnswered = 0;
        this.numberOfCorrect = 0;
    }

    public TournamentParticipantQuizAnswer(TournamentParticipantQuizAnswer other) {
        setQuizAnswerAggregateId(other.getQuizAnswerAggregateId());
        setQuizAnswerVersion(other.getQuizAnswerVersion());
        setAnswered(other.isAnswered());
        this.numberOfAnswered = other.getNumberOfAnswered();
        this.numberOfCorrect = other.getNumberOfCorrect();
    }


    public void updateAnswerWithQuestion(Integer quizAnswerAggregateId, Integer questionAnswerAggregateId, boolean isCorrect, Integer version) {
        if (!this.answered) {
            this.quizAnswerAggregateId = quizAnswerAggregateId;
            this.answered = true;
        }

        if (!this.quizAnswerAggregateId.equals(quizAnswerAggregateId)) {
            throw new TutorException(ErrorMessage.TOURNAMENT_PARTICIPANT_ADDING_ANSWER_WITH_WRONG_QUIZ_ANSWER_ID, quizAnswerAggregateId);
        }

        this.quizAnswerVersion = version;

        this.numberOfAnswered++;
        if (isCorrect) this.numberOfCorrect++;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public void setQuizAnswerAggregateId(Integer id) {
        this.quizAnswerAggregateId = id;
    }

    public Integer getQuizAnswerVersion() {
        return quizAnswerVersion;
    }

    public void setQuizAnswerVersion(Integer quizAnswerVersion) {
        this.quizAnswerVersion = quizAnswerVersion;
    }


    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public Integer getNumberOfAnswered() {
        return numberOfAnswered;
    }

    public void setNumberOfAnswered(Integer numberOfAnswered) {
        this.numberOfAnswered = numberOfAnswered;
    }

    public Integer getNumberOfCorrect() {
        return this.numberOfCorrect;
    }

    public void setNumberOfCorrect(Integer numberOfCorrect) {
        this.numberOfCorrect = numberOfCorrect;
    }

    public TournamentParticipant getTournamentParticipant() {
        return tournamentParticipant;
    }

    public void setTournamentParticipant(TournamentParticipant tournamentParticipant) {
        this.tournamentParticipant = tournamentParticipant;
    }
}
