package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfig;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;

@Entity
public class SagaPriceConfig extends PriceConfig implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaPriceConfig() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaPriceConfig(SagaPriceConfig other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaPriceConfig(Integer aggregateId, PriceConfigDto priceconfigDto) {
        super(aggregateId, priceconfigDto);
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