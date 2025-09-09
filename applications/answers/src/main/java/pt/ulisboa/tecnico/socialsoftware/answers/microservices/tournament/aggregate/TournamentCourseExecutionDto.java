package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentCourseExecutionDto implements Serializable {
	private Integer courseExecutionAggregateId;
	private Integer courseExecutionCourseId;
	private String courseExecutionAcronym;
	private String courseExecutionStatus;
	private Integer courseExecutionVersion;
	private Object tournament;

	public TournamentCourseExecutionDto() {
	}

	public TournamentCourseExecutionDto(TournamentCourseExecution tournamentcourseexecution) {
		this.courseExecutionAggregateId = tournamentcourseexecution.getCourseExecutionAggregateId();
		this.courseExecutionCourseId = tournamentcourseexecution.getCourseExecutionCourseId();
		this.courseExecutionAcronym = tournamentcourseexecution.getCourseExecutionAcronym();
		this.courseExecutionStatus = tournamentcourseexecution.getCourseExecutionStatus();
		this.courseExecutionVersion = tournamentcourseexecution.getCourseExecutionVersion();
		this.tournament = tournamentcourseexecution.getTournament();
	}

	public Integer getCourseExecutionAggregateId() {
		return courseExecutionAggregateId;
	}

	public void setCourseExecutionAggregateId(Integer courseExecutionAggregateId) {
		this.courseExecutionAggregateId = courseExecutionAggregateId;
	}

	public Integer getCourseExecutionCourseId() {
		return courseExecutionCourseId;
	}

	public void setCourseExecutionCourseId(Integer courseExecutionCourseId) {
		this.courseExecutionCourseId = courseExecutionCourseId;
	}

	public String getCourseExecutionAcronym() {
		return courseExecutionAcronym;
	}

	public void setCourseExecutionAcronym(String courseExecutionAcronym) {
		this.courseExecutionAcronym = courseExecutionAcronym;
	}

	public String getCourseExecutionStatus() {
		return courseExecutionStatus;
	}

	public void setCourseExecutionStatus(String courseExecutionStatus) {
		this.courseExecutionStatus = courseExecutionStatus;
	}

	public Integer getCourseExecutionVersion() {
		return courseExecutionVersion;
	}

	public void setCourseExecutionVersion(Integer courseExecutionVersion) {
		this.courseExecutionVersion = courseExecutionVersion;
	}

	public Object getTournament() {
		return tournament;
	}

	public void setTournament(Object tournament) {
		this.tournament = tournament;
	}

}