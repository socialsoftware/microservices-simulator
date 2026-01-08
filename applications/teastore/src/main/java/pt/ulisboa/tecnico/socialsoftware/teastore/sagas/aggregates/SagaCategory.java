package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

@Entity
public class SagaCategory extends Category implements SagaAggregate {
    private SagaState sagaState;

    public SagaCategory() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCategory(SagaCategory other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaCategory(Integer aggregateId, CategoryDto categoryDto) {
        super(aggregateId, categoryDto);
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