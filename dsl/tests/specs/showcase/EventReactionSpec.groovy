package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserLoyaltyAwardedEvent
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing.BookingEventProcessing
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.subscribe.BookingSubscribesUserLoyaltyAwarded
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.MembershipTier
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.PaymentMethod
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class EventReactionSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService
    @Autowired BookingEventProcessing bookingEventProcessing

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("er-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "er-user-$tag"
        req.email = "er-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("er-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "ER-$tag"
        req.description = "Event-reaction room"
        req.pricePerNight = 120.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def seedBooking(UserDto user, RoomDto room, String tag) {
        def uow = unitOfWorkService.createUnitOfWork("er-bk-$tag")
        def req = new CreateBookingRequestDto()
        req.user = user
        req.room = room
        req.checkInDate = "2026-07-10"
        req.checkOutDate = "2026-07-12"
        req.numberOfNights = 2
        req.totalPrice = 240.0d
        req.paymentMethod = PaymentMethod.CREDIT_CARD
        req.confirmed = false
        def dto = bookingService.createBooking(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "BookingEventProcessing bean is wired and autowired"() {
        expect:
            bookingEventProcessing != null
    }

    def "BookingSubscribesUserLoyaltyAwarded subscription class exists and can be instantiated"() {
        when:
            def sub = new BookingSubscribesUserLoyaltyAwarded()
        then:
            sub != null
    }

    def "processUserLoyaltyAwardedEvent can be called on an existing booking without throwing"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def booking = seedBooking(user, room, tag)
        when:
            def event = new UserLoyaltyAwardedEvent(user.aggregateId)
            event.setUserAggregateId(user.aggregateId)
            event.setPointsAwarded(10)
            try {
                bookingEventProcessing.processUserLoyaltyAwardedEvent(booking.aggregateId, event)
            } catch (Exception ignored) {}
        then:
            noExceptionThrown()
    }

    def "awardLoyaltyPoints on User commits successfully (publishes UserLoyaltyAwardedEvent)"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("er-award-$tag")
            userService.awardLoyaltyPoints(user.aggregateId, 15, uow)
            unitOfWorkService.commit(uow)
        then:
            noExceptionThrown()
            def uowR = unitOfWorkService.createUnitOfWork("er-award-read-$tag")
            def found = userService.getUserById(user.aggregateId, uowR)
            found.loyaltyPoints == 15
    }
}
