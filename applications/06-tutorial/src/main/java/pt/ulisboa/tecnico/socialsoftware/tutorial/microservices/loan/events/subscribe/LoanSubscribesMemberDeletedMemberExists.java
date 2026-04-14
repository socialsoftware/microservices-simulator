package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanMember;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.MemberDeletedEvent;


public class LoanSubscribesMemberDeletedMemberExists extends EventSubscription {
    public LoanSubscribesMemberDeletedMemberExists(LoanMember member) {
        super(member.getMemberAggregateId(),
                member.getMemberVersion(),
                MemberDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
