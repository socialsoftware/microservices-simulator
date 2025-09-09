package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class TournamentTopicDto implements Serializable {
	private Integer topicAggregateId;
	private String topicName;
	private Integer topicCourseAggregateId;
	private Integer topicVersion;
	private AggregateState state;
	private Object tournament;

	public TournamentTopicDto() {
	}

	public TournamentTopicDto(TournamentTopic tournamenttopic) {
		this.topicAggregateId = tournamenttopic.getTopicAggregateId();
		this.topicName = tournamenttopic.getTopicName();
		this.topicCourseAggregateId = tournamenttopic.getTopicCourseAggregateId();
		this.topicVersion = tournamenttopic.getTopicVersion();
		this.state = tournamenttopic.getState();
		this.tournament = tournamenttopic.getTournament();
	}

	public Integer getTopicAggregateId() {
		return topicAggregateId;
	}

	public void setTopicAggregateId(Integer topicAggregateId) {
		this.topicAggregateId = topicAggregateId;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public Integer getTopicCourseAggregateId() {
		return topicCourseAggregateId;
	}

	public void setTopicCourseAggregateId(Integer topicCourseAggregateId) {
		this.topicCourseAggregateId = topicCourseAggregateId;
	}

	public Integer getTopicVersion() {
		return topicVersion;
	}

	public void setTopicVersion(Integer topicVersion) {
		this.topicVersion = topicVersion;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}

	public Object getTournament() {
		return tournament;
	}

	public void setTournament(Object tournament) {
		this.tournament = tournament;
	}

}