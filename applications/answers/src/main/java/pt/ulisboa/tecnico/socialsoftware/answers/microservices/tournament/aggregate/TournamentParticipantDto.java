package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentParticipantDto implements Serializable {
	private Integer participantAggregateId;
	private String participantName;
	private String participantUsername;
	private LocalDateTime enrollTime;
	private TournamentParticipantQuizAnswer participantAnswer;
	private Integer participantVersion;
	private AggregateState state;
	private Tournament tournament;

	public TournamentParticipantDto() {
	}

	public TournamentParticipantDto(TournamentParticipant tournamentparticipant) {
		this.participantAggregateId = tournamentparticipant.getParticipantAggregateId();
		this.participantName = tournamentparticipant.getParticipantName();
		this.participantUsername = tournamentparticipant.getParticipantUsername();
		this.enrollTime = tournamentparticipant.getEnrollTime();
		this.participantAnswer = tournamentparticipant.getParticipantAnswer();
		this.participantVersion = tournamentparticipant.getParticipantVersion();
		this.state = tournamentparticipant.getState();
		this.tournament = tournamentparticipant.getTournament();
	}

	public Integer getParticipantAggregateId() {
		return participantAggregateId;
	}

	public void setParticipantAggregateId(Integer participantAggregateId) {
		this.participantAggregateId = participantAggregateId;
	}

	public String getParticipantName() {
		return participantName;
	}

	public void setParticipantName(String participantName) {
		this.participantName = participantName;
	}

	public String getParticipantUsername() {
		return participantUsername;
	}

	public void setParticipantUsername(String participantUsername) {
		this.participantUsername = participantUsername;
	}

	public LocalDateTime getEnrollTime() {
		return enrollTime;
	}

	public void setEnrollTime(LocalDateTime enrollTime) {
		this.enrollTime = enrollTime;
	}

	public TournamentParticipantQuizAnswer getParticipantAnswer() {
		return participantAnswer;
	}

	public void setParticipantAnswer(TournamentParticipantQuizAnswer participantAnswer) {
		this.participantAnswer = participantAnswer;
	}

	public Integer getParticipantVersion() {
		return participantVersion;
	}

	public void setParticipantVersion(Integer participantVersion) {
		this.participantVersion = participantVersion;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}

	public Tournament getTournament() {
		return tournament;
	}

	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
	}

}