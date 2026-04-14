package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto;
import java.util.List;

@Service
public class BookingFunctionalities {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public BookingDto createBooking(CreateBookingRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateBookingFunctionalitySagas createBookingFunctionalitySagas = new CreateBookingFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createBookingFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createBookingFunctionalitySagas.getCreatedBookingDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public BookingDto getBookingById(Integer bookingAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetBookingByIdFunctionalitySagas getBookingByIdFunctionalitySagas = new GetBookingByIdFunctionalitySagas(
                        sagaUnitOfWorkService, bookingAggregateId, sagaUnitOfWork, commandGateway);
                getBookingByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getBookingByIdFunctionalitySagas.getBookingDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public BookingDto updateBooking(BookingDto bookingDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(bookingDto);
                UpdateBookingFunctionalitySagas updateBookingFunctionalitySagas = new UpdateBookingFunctionalitySagas(
                        sagaUnitOfWorkService, bookingDto, sagaUnitOfWork, commandGateway);
                updateBookingFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateBookingFunctionalitySagas.getUpdatedBookingDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteBooking(Integer bookingAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteBookingFunctionalitySagas deleteBookingFunctionalitySagas = new DeleteBookingFunctionalitySagas(
                        sagaUnitOfWorkService, bookingAggregateId, sagaUnitOfWork, commandGateway);
                deleteBookingFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<BookingDto> getAllBookings() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllBookingsFunctionalitySagas getAllBookingsFunctionalitySagas = new GetAllBookingsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllBookingsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllBookingsFunctionalitySagas.getBookings();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(BookingDto bookingDto) {
        if (bookingDto.getCheckInDate() == null) {
            throw new ShowcaseException(BOOKING_MISSING_CHECKINDATE);
        }
        if (bookingDto.getCheckOutDate() == null) {
            throw new ShowcaseException(BOOKING_MISSING_CHECKOUTDATE);
        }
}

    private void checkInput(CreateBookingRequestDto createRequest) {
        if (createRequest.getCheckInDate() == null) {
            throw new ShowcaseException(BOOKING_MISSING_CHECKINDATE);
        }
        if (createRequest.getCheckOutDate() == null) {
            throw new ShowcaseException(BOOKING_MISSING_CHECKOUTDATE);
        }
}
}