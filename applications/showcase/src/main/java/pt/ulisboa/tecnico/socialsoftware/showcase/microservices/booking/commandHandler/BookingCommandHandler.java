package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.booking.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService;

import java.util.logging.Logger;

@Component
public class BookingCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(BookingCommandHandler.class.getName());

    @Autowired
    private BookingService bookingService;

    @Override
    protected String getAggregateTypeName() {
        return "Booking";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateBookingCommand cmd -> handleCreateBooking(cmd);
            case GetBookingByIdCommand cmd -> handleGetBookingById(cmd);
            case GetAllBookingsCommand cmd -> handleGetAllBookings(cmd);
            case UpdateBookingCommand cmd -> handleUpdateBooking(cmd);
            case DeleteBookingCommand cmd -> handleDeleteBooking(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateBooking(CreateBookingCommand cmd) {
        logger.info("handleCreateBooking");
        try {
            return bookingService.createBooking(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetBookingById(GetBookingByIdCommand cmd) {
        logger.info("handleGetBookingById");
        try {
            return bookingService.getBookingById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllBookings(GetAllBookingsCommand cmd) {
        logger.info("handleGetAllBookings");
        try {
            return bookingService.getAllBookings(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateBooking(UpdateBookingCommand cmd) {
        logger.info("handleUpdateBooking");
        try {
            return bookingService.updateBooking(cmd.getBookingDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteBooking(DeleteBookingCommand cmd) {
        logger.info("handleDeleteBooking");
        try {
            bookingService.deleteBooking(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
