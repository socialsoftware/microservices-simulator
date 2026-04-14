package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.commandHandler;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.grpc.GrpcCommandHandler;

@Component
@Profile("grpc")
public class BookingGrpcCommandHandler extends GrpcCommandHandler {

    private final BookingCommandHandler bookingCommandHandler;

    public BookingGrpcCommandHandler(MessagingObjectMapperProvider mapperProvider,
            BookingCommandHandler bookingCommandHandler) {
        super(mapperProvider);
        this.bookingCommandHandler = bookingCommandHandler;
    }

    @Override
    protected String getAggregateTypeName() {
        return "Booking";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return bookingCommandHandler.handleDomainCommand(command);
    }
}
