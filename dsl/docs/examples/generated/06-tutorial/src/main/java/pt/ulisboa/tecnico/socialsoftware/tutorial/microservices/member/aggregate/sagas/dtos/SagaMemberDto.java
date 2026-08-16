package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas.SagaMember;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaMemberDto extends MemberDto {
@Convert(converter = SagaStateConverter.class)
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