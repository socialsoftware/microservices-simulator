package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;

@Entity
public class SagaPost extends Post implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaPost() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaPost(SagaPost other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaPost(Integer aggregateId, PostDto postDto) {
        super(aggregateId, postDto);
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