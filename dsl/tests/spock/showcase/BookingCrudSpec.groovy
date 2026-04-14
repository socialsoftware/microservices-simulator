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
class BookingCrudSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("bkc-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "bkc-user-$tag"
        req.email = "bkc-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("bkc-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "BKC-$tag"
        req.description = "Booking test room"
        req.pricePerNight = 120.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def makeReq(UserDto user, RoomDto room, String tag, Double totalPrice = 240.0d, Integer nights = 2, Boolean confirmed = false) {
        def req = new CreateBookingRequestDto()
        req.user = user
        req.room = room
        req.checkInDate = "2026-05-10"
        req.checkOutDate = "2026-05-12"
        req.numberOfNights = nights
        req.totalPrice = totalPrice
        req.paymentMethod = PaymentMethod.CREDIT_CARD
        req.confirmed = confirmed
        return req
    }

    def "createBooking persists and getBookingById reads back"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bkc-create-$tag")
            def req = makeReq(user, room, tag)
            def created = bookingService.createBooking(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bkc-create-read-$tag")
            def found = bookingService.getBookingById(created.aggregateId, uowR)
            found.checkInDate == "2026-05-10"
            found.checkOutDate == "2026-05-12"
            found.numberOfNights == 2
            found.totalPrice == 240.0d
            found.paymentMethod == "CREDIT_CARD"
            found.confirmed == false
    }

    def "projection BookingDto.user carries username from underlying User"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bkc-proj-$tag")
            def created = bookingService.createBooking(makeReq(user, room, tag), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bkc-proj-read-$tag")
            def found = bookingService.getBookingById(created.aggregateId, uowR)
            found.user != null
            found.user.username == user.username
            found.user.aggregateId == user.aggregateId
    }

    def "projection BookingDto.room carries roomNumber and pricePerNight"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bkc-rproj-$tag")
            def created = bookingService.createBooking(makeReq(user, room, tag), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bkc-rproj-read-$tag")
            def found = bookingService.getBookingById(created.aggregateId, uowR)
            found.room != null
            found.room.roomNumber == room.roomNumber
            found.room.pricePerNight == room.pricePerNight
            found.room.aggregateId == room.aggregateId
    }

    def "updateBooking flips confirmed to true"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowC = unitOfWorkService.createUnitOfWork("bkc-upd-create-$tag")
            def created = bookingService.createBooking(makeReq(user, room, tag), uowC)
            unitOfWorkService.commit(uowC)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("bkc-upd-get-$tag")
            def existing = bookingService.getBookingById(created.aggregateId, uowGet)
            existing.confirmed = true
            def uowPut = unitOfWorkService.createUnitOfWork("bkc-upd-put-$tag")
            bookingService.updateBooking(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bkc-upd-read-$tag")
            def reloaded = bookingService.getBookingById(created.aggregateId, uowR)
            reloaded.confirmed == true
    }

    def "getAllBookings returns created bookings"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowA = unitOfWorkService.createUnitOfWork("bkc-all-a-$tag")
            def bookingA = bookingService.createBooking(makeReq(user, room, tag), uowA)
            unitOfWorkService.commit(uowA)
            def uowB = unitOfWorkService.createUnitOfWork("bkc-all-b-$tag")
            def bookingB = bookingService.createBooking(makeReq(user, room, tag, 360.0d, 3, false), uowB)
            unitOfWorkService.commit(uowB)
        when:
            def uow = unitOfWorkService.createUnitOfWork("bkc-all-$tag")
            def all = bookingService.getAllBookings(uow)
        then:
            all.any { it.aggregateId == bookingA.aggregateId }
            all.any { it.aggregateId == bookingB.aggregateId }
    }

    def "deleteBooking hides the row from subsequent reads"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowC = unitOfWorkService.createUnitOfWork("bkc-del-create-$tag")
            def created = bookingService.createBooking(makeReq(user, room, tag), uowC)
            unitOfWorkService.commit(uowC)
        when:
            def uowDel = unitOfWorkService.createUnitOfWork("bkc-del-$tag")
            bookingService.deleteBooking(created.aggregateId, uowDel)
            unitOfWorkService.commit(uowDel)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("bkc-del-read-$tag")
            try {
                bookingService.getBookingById(created.aggregateId, uowR)
                assert false : "expected to throw"
            } catch (Exception ignored) {}
    }
}
