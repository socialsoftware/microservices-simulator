package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;

@Entity
public class SagaBook extends Book implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.tutorial.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaBook() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaBook(SagaBook other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaBook(Integer aggregateId, BookDto bookDto) {
        super(aggregateId, bookDto);
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