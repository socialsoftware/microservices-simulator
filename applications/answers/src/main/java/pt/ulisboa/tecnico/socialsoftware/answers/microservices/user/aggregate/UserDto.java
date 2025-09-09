package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class UserDto implements Serializable {
	private Integer aggregateId;
	private String name;
	private String username;
	private boolean active;
	private Integer version;
	private AggregateState state;

	public UserDto() {
	}

	public UserDto(User user) {
		this.aggregateId = user.getAggregateId();
		this.name = user.getName();
		this.username = user.getUsername();
		this.active = user.isActive();
		this.version = user.getVersion();
		this.state = user.getState();
	}

	public Integer getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(Integer aggregateId) {
		this.aggregateId = aggregateId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}
}