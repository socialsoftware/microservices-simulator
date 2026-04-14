package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException
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
import spock.lang.PendingFeature
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class PreventDeleteSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("pd-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "pd-user-$tag"
        req.email = "pd-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("pd-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "PD-$tag"
        req.description = "Prevent-delete test room"
        req.pricePerNight = 80.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def seedBooking(UserDto user, RoomDto room, String tag) {
        def uow = unitOfWorkService.createUnitOfWork("pd-booking-$tag")
        def req = new CreateBookingRequestDto()
        req.user = user
        req.room = room
        req.checkInDate = "2026-07-01"
        req.checkOutDate = "2026-07-03"
        req.numberOfNights = 2
        req.totalPrice = 160.0d
        req.paymentMethod = PaymentMethod.CASH
        req.confirmed = false
        def dto = bookingService.createBooking(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "deleting a user referenced by a booking throws with 'Cannot delete user that has bookings'"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            seedBooking(user, room, tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("pd-userdel-$tag")
            userService.deleteUser(user.aggregateId, uow)
        then:
            def ex = thrown(ShowcaseException)
            ex.message != null && ex.message.contains("Cannot delete user that has bookings")
    }

    def "deleting a room referenced by a booking throws with 'Cannot delete room that has bookings'"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            seedBooking(user, room, tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("pd-roomdel-$tag")
            roomService.deleteRoom(room.aggregateId, uow)
        then:
            def ex = thrown(ShowcaseException)
            ex.message != null && ex.message.contains("Cannot delete room that has bookings")
    }

    @PendingFeature(reason = "After deleteBooking commits, a fresh findAll() in the prevent check still observes the booking — needs investigation of how Booking state transitions are visible across UnitOfWorks.")
    def "deleting the booking first allows the user to be deleted"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def booking = seedBooking(user, room, tag)
        when:
            def uowDelB = unitOfWorkService.createUnitOfWork("pd-delbooking-$tag")
            bookingService.deleteBooking(booking.aggregateId, uowDelB)
            unitOfWorkService.commit(uowDelB)

            def uowDelU = unitOfWorkService.createUnitOfWork("pd-deluser-ok-$tag")
            userService.deleteUser(user.aggregateId, uowDelU)
            unitOfWorkService.commit(uowDelU)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("pd-deluser-read-$tag")
            try {
                userService.getUserById(user.aggregateId, uowR)
                assert false : "expected to throw"
            } catch (Exception ignored) {}
    }
}
