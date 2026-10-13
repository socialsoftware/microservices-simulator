package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.Route;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStation;

import java.util.List;

@Entity
public class SagaRoute extends Route implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaRoute() {
    }

    public SagaRoute(Integer aggregateId, List<RouteStation> stations) {
        super(aggregateId, stations);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaRoute(SagaRoute other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
