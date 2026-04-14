package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.Discount;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;

@Entity
public class SagaDiscount extends Discount implements SagaAggregate {
    private SagaState sagaState;

    public SagaDiscount() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaDiscount(SagaDiscount other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaDiscount(Integer aggregateId, DiscountDto discountDto) {
        super(aggregateId, discountDto);
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