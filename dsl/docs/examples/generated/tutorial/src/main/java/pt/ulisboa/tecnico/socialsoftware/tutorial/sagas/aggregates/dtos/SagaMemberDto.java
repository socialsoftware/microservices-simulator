package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.SagaMember;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaMemberDto extends MemberDto {
private SagaState sagaState;

public SagaMemberDto(Member member) {
super((Member) member);
this.sagaState = ((SagaMember)member).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}