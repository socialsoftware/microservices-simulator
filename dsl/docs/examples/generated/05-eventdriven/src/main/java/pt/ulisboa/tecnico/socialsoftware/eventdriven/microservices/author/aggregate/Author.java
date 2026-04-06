package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Author extends Aggregate {
    private String name;
    private String bio;

    public Author() {

    }

    public Author(Integer aggregateId, AuthorDto authorDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(authorDto.getName());
        setBio(authorDto.getBio());
    }


    public Author(Author other) {
        super(other);
        setName(other.getName());
        setBio(other.getBio());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantNameNotBlank() {
        return this.name != null && this.name.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantNameNotBlank()) {
            throw new SimulatorException(INVARIANT_BREAK, "Author name cannot be blank");
        }
    }

    public AuthorDto buildDto() {
        AuthorDto dto = new AuthorDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setBio(getBio());
        return dto;
    }
}