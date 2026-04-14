package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingUser;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserDeletedEvent;


public class BookingSubscribesUserDeletedUserMustExist extends EventSubscription {
    public BookingSubscribesUserDeletedUserMustExist(BookingUser user) {
        super(user.getUserAggregateId(),
                user.getUserVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
