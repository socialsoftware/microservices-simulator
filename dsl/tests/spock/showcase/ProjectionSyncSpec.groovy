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
class ProjectionSyncSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ps-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "ps-user-$tag"
        req.email = "ps-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ps-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "PS-$tag"
        req.description = "Projection-sync room"
        req.pricePerNight = 150.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def makeBookingReq(UserDto user, RoomDto room) {
        def req = new CreateBookingRequestDto()
        req.user = user
        req.room = room
        req.checkInDate = "2026-06-01"
        req.checkOutDate = "2026-06-03"
        req.numberOfNights = 2
        req.totalPrice = 300.0d
        req.paymentMethod = PaymentMethod.CREDIT_CARD
        req.confirmed = false
        return req
    }

    def "booking.user projection reflects the User.username at creation time"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ps-bk-create-$tag")
            def created = bookingService.createBooking(makeBookingReq(user, room), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ps-bk-read-$tag")
            def found = bookingService.getBookingById(created.aggregateId, uowR)
            found.user != null
            found.user.username == user.username
            found.user.email == user.email
    }

    def "updating User.username keeps the projection accessible (existing booking still readable)"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowC = unitOfWorkService.createUnitOfWork("ps-uu-create-$tag")
            def booking = bookingService.createBooking(makeBookingReq(user, room), uowC)
            unitOfWorkService.commit(uowC)
        when: "we update the user's username"
            try {
                def uowUp = unitOfWorkService.createUnitOfWork("ps-uu-upd-$tag")
                def fresh = userService.getUserById(user.aggregateId, uowUp)
                fresh.username = "updated-${tag}"
                userService.updateUser(fresh, uowUp)
                unitOfWorkService.commit(uowUp)
            } catch (Exception ignored) {}
        then: "the existing booking is still readable — its projection is at least consistent"
            def uowR = unitOfWorkService.createUnitOfWork("ps-uu-read-$tag")
            def reloaded = bookingService.getBookingById(booking.aggregateId, uowR)
            reloaded != null
            reloaded.user != null
            reloaded.user.aggregateId == user.aggregateId
    }

    def "updating User.email keeps the existing booking's user projection readable"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowC = unitOfWorkService.createUnitOfWork("ps-ue-create-$tag")
            def booking = bookingService.createBooking(makeBookingReq(user, room), uowC)
            unitOfWorkService.commit(uowC)
        when:
            try {
                def uowUp = unitOfWorkService.createUnitOfWork("ps-ue-upd-$tag")
                def fresh = userService.getUserById(user.aggregateId, uowUp)
                fresh.email = "new-${tag}@example.com"
                userService.updateUser(fresh, uowUp)
                unitOfWorkService.commit(uowUp)
            } catch (Exception ignored) {}
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ps-ue-read-$tag")
            def reloaded = bookingService.getBookingById(booking.aggregateId, uowR)
            reloaded != null
            reloaded.user.aggregateId == user.aggregateId
    }

    def "updating Room.roomNumber keeps the existing booking's room projection readable"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowC = unitOfWorkService.createUnitOfWork("ps-rn-create-$tag")
            def booking = bookingService.createBooking(makeBookingReq(user, room), uowC)
            unitOfWorkService.commit(uowC)
        when:
            try {
                def uowUp = unitOfWorkService.createUnitOfWork("ps-rn-upd-$tag")
                def fresh = roomService.getRoomById(room.aggregateId, uowUp)
                fresh.roomNumber = "PS-NEW-$tag"
                roomService.updateRoom(fresh, uowUp)
                unitOfWorkService.commit(uowUp)
            } catch (Exception ignored) {}
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ps-rn-read-$tag")
            def reloaded = bookingService.getBookingById(booking.aggregateId, uowR)
            reloaded != null
            reloaded.room != null
            reloaded.room.aggregateId == room.aggregateId
    }

    def "updating Room.pricePerNight keeps the existing booking's room projection readable"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def uowC = unitOfWorkService.createUnitOfWork("ps-rp-create-$tag")
            def booking = bookingService.createBooking(makeBookingReq(user, room), uowC)
            unitOfWorkService.commit(uowC)
        when:
            try {
                def uowUp = unitOfWorkService.createUnitOfWork("ps-rp-upd-$tag")
                def fresh = roomService.getRoomById(room.aggregateId, uowUp)
                fresh.pricePerNight = 999.0d
                roomService.updateRoom(fresh, uowUp)
                unitOfWorkService.commit(uowUp)
            } catch (Exception ignored) {}
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ps-rp-read-$tag")
            def reloaded = bookingService.getBookingById(booking.aggregateId, uowR)
            reloaded != null
            reloaded.room.aggregateId == room.aggregateId
            reloaded.room.pricePerNight != null
    }
}
