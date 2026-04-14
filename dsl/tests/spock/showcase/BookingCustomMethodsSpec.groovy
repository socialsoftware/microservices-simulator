package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto
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
class BookingCustomMethodsSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("bcm-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "bcm-user-$tag"
        req.email = "bcm-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("bcm-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "BCM-$tag"
        req.description = "Booking custom test room"
        req.pricePerNight = 100.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def seedBooking(UserDto user, RoomDto room, String tag, Boolean confirmed = false) {
        def uow = unitOfWorkService.createUnitOfWork("bcm-booking-$tag")
        def req = new CreateBookingRequestDto()
        req.user = user
        req.room = room
        req.checkInDate = "2026-06-01"
        req.checkOutDate = "2026-06-04"
        req.numberOfNights = 3
        req.totalPrice = 300.0d
        req.paymentMethod = PaymentMethod.CREDIT_CARD
        req.confirmed = confirmed
        def dto = bookingService.createBooking(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "confirmBooking flips confirmed to true"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def booking = seedBooking(user, room, tag, false)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bcm-confirm-$tag")
            bookingService.confirmBooking(booking.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bcm-confirm-read-$tag")
            def found = bookingService.getBookingById(booking.aggregateId, uowR)
            found.confirmed == true
    }

    def "confirmBooking is idempotent when booking already confirmed"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def booking = seedBooking(user, room, tag, true)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bcm-confirm-idem-$tag")
            bookingService.confirmBooking(booking.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bcm-confirm-idem-read-$tag")
            def found = bookingService.getBookingById(booking.aggregateId, uowR)
            found.confirmed == true
    }

    def "confirmBooking does not disturb other fields"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def booking = seedBooking(user, room, tag, false)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bcm-confirm-fields-$tag")
            bookingService.confirmBooking(booking.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bcm-confirm-fields-read-$tag")
            def found = bookingService.getBookingById(booking.aggregateId, uowR)
            found.confirmed == true
            found.numberOfNights == 3
            found.totalPrice == 300.0d
            found.checkInDate == "2026-06-01"
    }

}
