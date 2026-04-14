package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingUserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingRoomDto;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.PaymentMethod;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.BookingDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.BookingUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.Room;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;


@Service
@Transactional(noRollbackFor = ShowcaseException.class)
public class BookingService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingFactory bookingFactory;

    @Autowired
    private BookingServiceExtension extension;

    public BookingService() {}

    public BookingDto createBooking(CreateBookingRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            BookingDto bookingDto = new BookingDto();
            bookingDto.setCheckInDate(createRequest.getCheckInDate());
            bookingDto.setCheckOutDate(createRequest.getCheckOutDate());
            bookingDto.setNumberOfNights(createRequest.getNumberOfNights());
            bookingDto.setTotalPrice(createRequest.getTotalPrice());
            bookingDto.setPaymentMethod(createRequest.getPaymentMethod() != null ? createRequest.getPaymentMethod().name() : null);
            bookingDto.setConfirmed(createRequest.getConfirmed());
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                BookingUserDto userDto = new BookingUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setUsername(refSourceDto.getUsername());
                userDto.setEmail(refSourceDto.getEmail());
                bookingDto.setUser(userDto);
            }
            if (createRequest.getRoom() != null) {
                Room refSource = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getRoom().getAggregateId(), unitOfWork);
                RoomDto refSourceDto = new RoomDto(refSource);
                BookingRoomDto roomDto = new BookingRoomDto();
                roomDto.setAggregateId(refSourceDto.getAggregateId());
                roomDto.setVersion(refSourceDto.getVersion());
                roomDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                roomDto.setRoomNumber(refSourceDto.getRoomNumber());
                roomDto.setPricePerNight(refSourceDto.getPricePerNight());
                bookingDto.setRoom(roomDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Booking booking = bookingFactory.createBooking(aggregateId, bookingDto);
            unitOfWorkService.registerChanged(booking, unitOfWork);
            return bookingFactory.createBookingDto(booking);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error creating booking: " + e.getMessage());
        }
    }

    public BookingDto getBookingById(Integer id, UnitOfWork unitOfWork) {
        try {
            Booking booking = (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return bookingFactory.createBookingDto(booking);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving booking: " + e.getMessage());
        }
    }

    public List<BookingDto> getAllBookings(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = bookingRepository.findAll().stream()
                .map(Booking::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(bookingFactory::createBookingDto)
                .collect(Collectors.toList());
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving booking: " + e.getMessage());
        }
    }

    public BookingDto updateBooking(BookingDto bookingDto, UnitOfWork unitOfWork) {
        try {
            Integer id = bookingDto.getAggregateId();
            Booking oldBooking = (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Booking newBooking = bookingFactory.createBookingFromExisting(oldBooking);
            if (bookingDto.getCheckInDate() != null) {
                newBooking.setCheckInDate(bookingDto.getCheckInDate());
            }
            if (bookingDto.getCheckOutDate() != null) {
                newBooking.setCheckOutDate(bookingDto.getCheckOutDate());
            }
            if (bookingDto.getNumberOfNights() != null) {
                newBooking.setNumberOfNights(bookingDto.getNumberOfNights());
            }
            if (bookingDto.getTotalPrice() != null) {
                newBooking.setTotalPrice(bookingDto.getTotalPrice());
            }
            if (bookingDto.getPaymentMethod() != null) {
                newBooking.setPaymentMethod(PaymentMethod.valueOf(bookingDto.getPaymentMethod()));
            }
            newBooking.setConfirmed(bookingDto.getConfirmed());

            unitOfWorkService.registerChanged(newBooking, unitOfWork);            BookingUpdatedEvent event = new BookingUpdatedEvent(newBooking.getAggregateId(), newBooking.getCheckInDate(), newBooking.getCheckOutDate(), newBooking.getNumberOfNights(), newBooking.getTotalPrice(), newBooking.getConfirmed());
            event.setPublisherAggregateVersion(newBooking.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return bookingFactory.createBookingDto(newBooking);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error updating booking: " + e.getMessage());
        }
    }

    public void deleteBooking(Integer id, UnitOfWork unitOfWork) {
        try {
            Booking oldBooking = (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Booking newBooking = bookingFactory.createBookingFromExisting(oldBooking);
            newBooking.remove();
            unitOfWorkService.registerChanged(newBooking, unitOfWork);            unitOfWorkService.registerEvent(new BookingDeletedEvent(newBooking.getAggregateId()), unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error deleting booking: " + e.getMessage());
        }
    }




    public void handleUserLoyaltyAwardedEvent(Integer aggregateId, UnitOfWork unitOfWork) {
    }

    @Transactional
    public BookingDto bookRoom(BookingUser user, BookingRoom room, String checkIn, String checkOut, Integer nights, Double price, UnitOfWork unitOfWork) {
        try {
        BookingDto dto = new BookingDto();
        dto.setUser(user.buildDto());
        dto.setRoom(room.buildDto());
        dto.setCheckInDate(checkIn);
        dto.setCheckOutDate(checkOut);
        dto.setNumberOfNights(nights);
        dto.setTotalPrice(price);
        dto.setPaymentMethod("CREDIT_CARD");
        dto.setConfirmed(false);
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Booking booking = bookingFactory.createBooking(aggregateId, dto);
        unitOfWorkService.registerChanged(booking, unitOfWork);
        BookingCreatedEvent event0 = new BookingCreatedEvent();
        event0.setUserAggregateId(user.getUserAggregateId());
        event0.setRoomAggregateId(room.getRoomAggregateId());
        event0.setTotalPrice(price);
        event0.setPublisherAggregateVersion(booking.getVersion());
        unitOfWorkService.registerEvent(event0, unitOfWork);
        return bookingFactory.createBookingDto(booking);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in bookRoom Booking: " + e.getMessage());
        }
    }

    @Transactional
    public void confirmBooking(Integer bookingId, UnitOfWork unitOfWork) {
        try {
        Booking bookingOld = (Booking) unitOfWorkService.aggregateLoadAndRegisterRead(bookingId, unitOfWork);
        Booking booking = bookingFactory.createBookingFromExisting(bookingOld);
        booking.setConfirmed(true);
        unitOfWorkService.registerChanged(booking, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in confirmBooking Booking: " + e.getMessage());
        }
    }


}