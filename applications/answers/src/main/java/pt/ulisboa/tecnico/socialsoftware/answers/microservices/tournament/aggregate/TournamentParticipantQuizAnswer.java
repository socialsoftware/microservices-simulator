package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public class TournamentParticipantQuizAnswer {
    @Id
    @GeneratedValue
    private Long id;
    private Integer quizAnswerAggregateId;
    private Integer quizAnswerVersion;
    private Boolean answered;
    private Integer numberOfAnswered;
    private Integer numberOfCorrect;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipantquizanswer")
    private TournamentParticipant tournamentParticipant;
    @OneToOne
    private Tournament tournament; 

    public TournamentParticipantQuizAnswer() {
    }

    public TournamentParticipantQuizAnswer(TournamentDto tournamentDto) {
        setQuizAnswerAggregateId(tournamentDto.getQuizAnswerAggregateId());
        setQuizAnswerVersion(tournamentDto.getQuizAnswerVersion());
        setAnswered(tournamentDto.getAnswered());
        setNumberOfAnswered(tournamentDto.getNumberOfAnswered());
        setNumberOfCorrect(tournamentDto.getNumberOfCorrect());
        setTournamentParticipant(tournamentParticipant);
    }

    public TournamentParticipantQuizAnswer(TournamentParticipantQuizAnswer other) {
        setQuizAnswerAggregateId(other.getQuizAnswerAggregateId());
        setQuizAnswerVersion(other.getQuizAnswerVersion());
        setAnswered(other.getAnswered());
        setNumberOfAnswered(other.getNumberOfAnswered());
        setNumberOfCorrect(other.getNumberOfCorrect());
        setTournamentParticipant(new TournamentParticipant(other.getTournamentParticipant()));
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

    public TournamentParticipant getTournamentParticipant() {
        return tournamentParticipant;
    }

    public void setTournamentParticipant(TournamentParticipant tournamentParticipant) {
        this.tournamentParticipant = tournamentParticipant;
        if (this.tournamentParticipant != null) {
            this.tournamentParticipant.setTournamentParticipantQuizAnswer(this);
        }
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


}