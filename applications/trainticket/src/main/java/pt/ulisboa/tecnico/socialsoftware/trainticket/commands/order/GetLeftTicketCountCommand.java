package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;

import java.time.LocalDate;

public class GetLeftTicketCountCommand extends Command {
    private Integer tripAggregateId;
    private LocalDate travelDate;
    private SeatClass seatClass;
    private Integer capacity;

    public GetLeftTicketCountCommand(UnitOfWork unitOfWork, String serviceName, Integer tripAggregateId,
                                     LocalDate travelDate, SeatClass seatClass, Integer capacity) {
        super(unitOfWork, serviceName, null);
        this.tripAggregateId = tripAggregateId;
        this.travelDate = travelDate;
        this.seatClass = seatClass;
        this.capacity = capacity;
    }

    public Integer getTripAggregateId() {
        return tripAggregateId;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public Integer getCapacity() {
        return capacity;
    }
}
