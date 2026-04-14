package pt.ulisboa.tecnico.socialsoftware.showcase.command.booking;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;

public class UpdateBookingCommand extends Command {
    private final BookingDto bookingDto;

    public UpdateBookingCommand(UnitOfWork unitOfWork, String serviceName, BookingDto bookingDto) {
        super(unitOfWork, serviceName, null);
        this.bookingDto = bookingDto;
    }

    public BookingDto getBookingDto() { return bookingDto; }
}
