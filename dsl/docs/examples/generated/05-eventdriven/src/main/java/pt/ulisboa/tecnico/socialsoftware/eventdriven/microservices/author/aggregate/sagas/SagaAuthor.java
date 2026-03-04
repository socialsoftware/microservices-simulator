package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;

@Entity
public class SagaAuthor extends Author implements SagaAggregate {
    private SagaState sagaState;

    public SagaAuthor() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaAuthor(SagaAuthor other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaAuthor(Integer aggregateId, AuthorDto authorDto) {
        super(aggregateId, authorDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}