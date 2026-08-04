package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteDto;

@Entity
public class SagaRoute extends Route implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaRoute() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaRoute(SagaRoute other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaRoute(Integer aggregateId, RouteDto routeDto) {
        super(aggregateId, routeDto);
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