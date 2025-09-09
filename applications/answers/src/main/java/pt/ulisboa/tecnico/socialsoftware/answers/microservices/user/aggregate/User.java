package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Entity
public class User extends Aggregate {
    @Id
    private String name;
    private String username;
    private Boolean active; 

    public User(String name, String username, Boolean active) {
        this.name = name;
        this.username = username;
        this.active = active;
    }

    public User(User other) {
        // Copy constructor
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

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


}