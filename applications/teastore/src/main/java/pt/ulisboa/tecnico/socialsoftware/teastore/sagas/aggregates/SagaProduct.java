package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductCategory;

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

    public SagaProduct(Integer aggregateId, ProductDto productDto, ProductCategory productCategory) {
        super(aggregateId, productDto, productCategory);
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