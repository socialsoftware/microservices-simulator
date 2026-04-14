package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRepository
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.aggregate.UserRepository
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
class RepositoryQuerySpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService
    @Autowired UserRepository userRepository
    @Autowired BookingRepository bookingRepository

    private UserDto seedUser(String tag, Boolean active) {
        def uow = unitOfWorkService.createUnitOfWork("rq-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "rq-user-$tag"
        req.email = "rq-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = active
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("rq-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "RQ-$tag"
        req.description = "Repo query room"
        req.pricePerNight = 100.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def seedBooking(UserDto user, RoomDto room, String tag, Double totalPrice, Boolean confirmed) {
        def uow = unitOfWorkService.createUnitOfWork("rq-booking-$tag")
        def req = new CreateBookingRequestDto()
        req.user = user
        req.room = room
        req.checkInDate = "2026-08-10"
        req.checkOutDate = "2026-08-12"
        req.numberOfNights = 2
        req.totalPrice = totalPrice
        req.paymentMethod = PaymentMethod.CREDIT_CARD
        req.confirmed = confirmed
        def dto = bookingService.createBooking(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "findActiveUserIds returns only active users"() {
        given:
            def tag = "${System.nanoTime()}"
            def activeUser = seedUser("active-$tag", true)
            def inactiveUser = seedUser("inactive-$tag", false)
        when:
            def ids = userRepository.findActiveUserIds()
        then:
            ids.contains(activeUser.aggregateId)
            !ids.contains(inactiveUser.aggregateId)
    }

    def "findExpensiveBookingIds returns only bookings above the threshold"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser("exp-$tag", true)
            def room = seedRoom(tag)
            def cheap = seedBooking(user, room, "cheap-$tag", 50.0d, false)
            def mid = seedBooking(user, room, "mid-$tag", 200.0d, false)
            def expensive = seedBooking(user, room, "exp-$tag", 500.0d, false)
        when:
            def ids = bookingRepository.findExpensiveBookingIds(200.0d)
        then:
            ids.contains(mid.aggregateId)
            ids.contains(expensive.aggregateId)
            !ids.contains(cheap.aggregateId)
    }

    def "findConfirmedBookingIds returns only confirmed bookings"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser("cnf-$tag", true)
            def room = seedRoom("cnf-$tag")
            def unconfirmed = seedBooking(user, room, "u-$tag", 100.0d, false)
            def confirmed = seedBooking(user, room, "c-$tag", 100.0d, true)
        when:
            def ids = bookingRepository.findConfirmedBookingIds()
        then:
            ids.contains(confirmed.aggregateId)
            !ids.contains(unconfirmed.aggregateId)
    }
}
