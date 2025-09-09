package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentCreatorDto implements Serializable {
	private Integer creatorAggregateId;
	private String creatorName;
	private String creatorUsername;
	private Integer creatorVersion;
	private AggregateState creatorState;
	private Object tournament;

	public TournamentCreatorDto() {
	}

	public TournamentCreatorDto(TournamentCreator tournamentcreator) {
		this.creatorAggregateId = tournamentcreator.getCreatorAggregateId();
		this.creatorName = tournamentcreator.getCreatorName();
		this.creatorUsername = tournamentcreator.getCreatorUsername();
		this.creatorVersion = tournamentcreator.getCreatorVersion();
		this.creatorState = tournamentcreator.getCreatorState();
		this.tournament = tournamentcreator.getTournament();
	}

	public Integer getCreatorAggregateId() {
		return creatorAggregateId;
	}

	public void setCreatorAggregateId(Integer creatorAggregateId) {
		this.creatorAggregateId = creatorAggregateId;
	}

	public String getCreatorName() {
		return creatorName;
	}

	public void setCreatorName(String creatorName) {
		this.creatorName = creatorName;
	}

	public String getCreatorUsername() {
		return creatorUsername;
	}

	public void setCreatorUsername(String creatorUsername) {
		this.creatorUsername = creatorUsername;
	}

	public Integer getCreatorVersion() {
		return creatorVersion;
	}

	public void setCreatorVersion(Integer creatorVersion) {
		this.creatorVersion = creatorVersion;
	}

	public AggregateState getCreatorState() {
		return creatorState;
	}

	public void setCreatorState(AggregateState creatorState) {
		this.creatorState = creatorState;
	}

	public Object getTournament() {
		return tournament;
	}

	public void setTournament(Object tournament) {
		this.tournament = tournament;
	}

}