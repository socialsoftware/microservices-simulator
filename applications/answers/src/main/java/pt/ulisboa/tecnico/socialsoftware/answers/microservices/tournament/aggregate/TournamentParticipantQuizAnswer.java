package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Embeddable;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Embeddable
public class TournamentParticipantQuizAnswer {
    private Long id;
    private Integer quizAnswerAggregateId;
    private Integer quizAnswerVersion;
    private Boolean answered;
    private Integer numberOfAnswered;
    private Integer numberOfCorrect;
    private Object tournamentParticipant; 

    public TournamentParticipantQuizAnswer(Long id, Integer quizAnswerAggregateId, Integer quizAnswerVersion, Boolean answered, Integer numberOfAnswered, Integer numberOfCorrect, Object tournamentParticipant) {
        this.id = id;
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.quizAnswerVersion = quizAnswerVersion;
        this.answered = answered;
        this.numberOfAnswered = numberOfAnswered;
        this.numberOfCorrect = numberOfCorrect;
        this.tournamentParticipant = tournamentParticipant;
    }

    public TournamentParticipantQuizAnswer(TournamentParticipantQuizAnswer other) {
        // Copy constructor
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public void setQuizAnswerAggregateId(Integer quizAnswerAggregateId) {
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Integer getQuizAnswerVersion() {
        return quizAnswerVersion;
    }

    public void setQuizAnswerVersion(Integer quizAnswerVersion) {
        this.quizAnswerVersion = quizAnswerVersion;
    }

    public Boolean isAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Integer getNumberOfAnswered() {
        return numberOfAnswered;
    }

    public void setNumberOfAnswered(Integer numberOfAnswered) {
        this.numberOfAnswered = numberOfAnswered;
    }

    public Integer getNumberOfCorrect() {
        return numberOfCorrect;
    }

    public void setNumberOfCorrect(Integer numberOfCorrect) {
        this.numberOfCorrect = numberOfCorrect;
    }

    public Object getTournamentParticipant() {
        return tournamentParticipant;
    }

    public void setTournamentParticipant(Object tournamentParticipant) {
        this.tournamentParticipant = tournamentParticipant;
    }


}