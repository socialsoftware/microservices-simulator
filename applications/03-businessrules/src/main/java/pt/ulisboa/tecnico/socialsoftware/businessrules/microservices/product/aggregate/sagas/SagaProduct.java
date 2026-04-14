package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.businessrules.shared.dtos.ProductDto;

@Entity
public class SagaProduct extends Product implements SagaAggregate {
    private SagaState sagaState;

    public SagaProduct() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaProduct(SagaProduct other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaProduct(Integer aggregateId, ProductDto productDto) {
        super(aggregateId, productDto);
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