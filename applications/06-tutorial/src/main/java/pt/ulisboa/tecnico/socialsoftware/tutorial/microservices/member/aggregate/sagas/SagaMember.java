package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;

@Entity
public class SagaMember extends Member implements SagaAggregate {
    @jakarta.persistence.Convert(converter = pt.ulisboa.tecnico.socialsoftware.tutorial.shared.sagaStates.SagaStateConverter.class)
    private SagaState sagaState;

    public SagaMember() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaMember(SagaMember other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaMember(Integer aggregateId, MemberDto memberDto) {
        super(aggregateId, memberDto);
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