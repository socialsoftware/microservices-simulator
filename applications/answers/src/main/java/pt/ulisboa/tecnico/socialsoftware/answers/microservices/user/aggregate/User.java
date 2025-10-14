package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public abstract class User extends Aggregate {
    private String name;
    private String username;
    private Boolean active;

    public User() {
    }

    public User(Integer aggregateId, UserDto userDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(userDto.getName());
        setUsername(userDto.getUsername());
        setActive(userDto.getActive());
    }

    public User(User other) {
        super(other);
        setName(other.getName());
        setUsername(other.getUsername());
        setActive(other.getActive());
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


}