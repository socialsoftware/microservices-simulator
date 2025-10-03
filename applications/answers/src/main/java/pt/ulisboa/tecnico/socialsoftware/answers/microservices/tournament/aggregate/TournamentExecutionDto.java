package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentExecutionDto implements Serializable {
	private Integer executionAggregateId;
	private Integer executionCourseId;
	private String executionAcronym;
	private String executionStatus;
	private Integer executionVersion;
	private Tournament tournament;

	public TournamentExecutionDto() {
	}

	public TournamentExecutionDto(TournamentExecution tournamentexecution) {
		this.executionAggregateId = tournamentexecution.getExecutionAggregateId();
		this.executionCourseId = tournamentexecution.getExecutionCourseId();
		this.executionAcronym = tournamentexecution.getExecutionAcronym();
		this.executionStatus = tournamentexecution.getExecutionStatus();
		this.executionVersion = tournamentexecution.getExecutionVersion();
		this.tournament = tournamentexecution.getTournament();
	}

	public Integer getExecutionAggregateId() {
		return executionAggregateId;
	}

	public void setExecutionAggregateId(Integer executionAggregateId) {
		this.executionAggregateId = executionAggregateId;
	}

	public Integer getExecutionCourseId() {
		return executionCourseId;
	}

	public void setExecutionCourseId(Integer executionCourseId) {
		this.executionCourseId = executionCourseId;
	}

	public String getExecutionAcronym() {
		return executionAcronym;
	}

	public void setExecutionAcronym(String executionAcronym) {
		this.executionAcronym = executionAcronym;
	}

	public String getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(String executionStatus) {
		this.executionStatus = executionStatus;
	}

	public Integer getExecutionVersion() {
		return executionVersion;
	}

	public void setExecutionVersion(Integer executionVersion) {
		this.executionVersion = executionVersion;
	}

	public Tournament getTournament() {
		return tournament;
	}

	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
	}

}