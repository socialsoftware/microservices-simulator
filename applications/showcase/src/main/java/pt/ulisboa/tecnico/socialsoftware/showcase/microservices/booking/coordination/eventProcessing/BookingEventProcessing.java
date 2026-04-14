package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService;

@Service
public class BookingEventProcessing {
    @Autowired
    private BookingService bookingService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public BookingEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}