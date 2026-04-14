package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.RoomDeletedEvent;

@Service
public class BookingEventProcessing {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingFactory bookingFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public BookingEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Booking oldBooking = (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Booking newBooking = bookingFactory.createBookingFromExisting(oldBooking);
        newBooking.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newBooking, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processRoomDeletedEvent(Integer aggregateId, RoomDeletedEvent roomDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Booking oldBooking = (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Booking newBooking = bookingFactory.createBookingFromExisting(oldBooking);
        newBooking.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newBooking, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}