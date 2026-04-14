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
class InvariantsSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedValidUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("inv-vuser-$tag")
        def req = new CreateUserRequestDto()
        req.username = "inv-vuser-$tag"
        req.email = "inv-vuser-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedValidRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("inv-vroom-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "INV-$tag"
        req.description = "inv room"
        req.pricePerNight = 100.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

def "user with blank username violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-user-uname-${System.nanoTime()}")
            def req = new CreateUserRequestDto()
            req.username = ""
            req.email = "ok@example.com"
            req.loyaltyPoints = 0
            req.tier = MembershipTier.BRONZE
            req.active = true
        when:
            userService.createUser(req, uow)
        then:
            thrown(Exception)
    }

    def "user with blank email violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-user-email-${System.nanoTime()}")
            def req = new CreateUserRequestDto()
            req.username = "ok-${System.nanoTime()}"
            req.email = ""
            req.loyaltyPoints = 0
            req.tier = MembershipTier.BRONZE
            req.active = true
        when:
            userService.createUser(req, uow)
        then:
            thrown(Exception)
    }

    def "user with negative loyaltyPoints violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-user-lp-${System.nanoTime()}")
            def req = new CreateUserRequestDto()
            req.username = "loy-${System.nanoTime()}"
            req.email = "loy-${System.nanoTime()}@example.com"
            req.loyaltyPoints = -1
            req.tier = MembershipTier.BRONZE
            req.active = true
        when:
            userService.createUser(req, uow)
        then:
            thrown(Exception)
    }

def "room with blank roomNumber violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-room-num-${System.nanoTime()}")
            def req = new CreateRoomRequestDto()
            req.roomNumber = ""
            req.description = "ok"
            req.pricePerNight = 100.0d
            req.amenities = [] as Set
            req.status = RoomStatus.AVAILABLE
        when:
            roomService.createRoom(req, uow)
        then:
            thrown(Exception)
    }

    def "room with zero pricePerNight violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-room-price0-${System.nanoTime()}")
            def req = new CreateRoomRequestDto()
            req.roomNumber = "R-${System.nanoTime()}"
            req.description = "ok"
            req.pricePerNight = 0.0d
            req.amenities = [] as Set
            req.status = RoomStatus.AVAILABLE
        when:
            roomService.createRoom(req, uow)
        then:
            thrown(Exception)
    }

    def "room with negative pricePerNight violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-room-priceneg-${System.nanoTime()}")
            def req = new CreateRoomRequestDto()
            req.roomNumber = "R-${System.nanoTime()}"
            req.description = "ok"
            req.pricePerNight = -10.0d
            req.amenities = [] as Set
            req.status = RoomStatus.AVAILABLE
        when:
            roomService.createRoom(req, uow)
        then:
            thrown(Exception)
    }

def "booking with blank checkInDate violates invariant"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedValidUser(tag)
            def room = seedValidRoom(tag)
            def uow = unitOfWorkService.createUnitOfWork("inv-book-ci-$tag")
            def req = new CreateBookingRequestDto()
            req.user = user
            req.room = room
            req.checkInDate = ""
            req.checkOutDate = "2026-09-02"
            req.numberOfNights = 1
            req.totalPrice = 100.0d
            req.paymentMethod = PaymentMethod.CREDIT_CARD
            req.confirmed = false
        when:
            bookingService.createBooking(req, uow)
        then:
            thrown(Exception)
    }

    def "booking with blank checkOutDate violates invariant"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedValidUser(tag)
            def room = seedValidRoom(tag)
            def uow = unitOfWorkService.createUnitOfWork("inv-book-co-$tag")
            def req = new CreateBookingRequestDto()
            req.user = user
            req.room = room
            req.checkInDate = "2026-09-01"
            req.checkOutDate = ""
            req.numberOfNights = 1
            req.totalPrice = 100.0d
            req.paymentMethod = PaymentMethod.CREDIT_CARD
            req.confirmed = false
        when:
            bookingService.createBooking(req, uow)
        then:
            thrown(Exception)
    }

    def "booking with zero numberOfNights violates invariant"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedValidUser(tag)
            def room = seedValidRoom(tag)
            def uow = unitOfWorkService.createUnitOfWork("inv-book-n0-$tag")
            def req = new CreateBookingRequestDto()
            req.user = user
            req.room = room
            req.checkInDate = "2026-09-01"
            req.checkOutDate = "2026-09-02"
            req.numberOfNights = 0
            req.totalPrice = 100.0d
            req.paymentMethod = PaymentMethod.CREDIT_CARD
            req.confirmed = false
        when:
            bookingService.createBooking(req, uow)
        then:
            thrown(Exception)
    }

    def "booking with zero totalPrice violates invariant"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedValidUser(tag)
            def room = seedValidRoom(tag)
            def uow = unitOfWorkService.createUnitOfWork("inv-book-p0-$tag")
            def req = new CreateBookingRequestDto()
            req.user = user
            req.room = room
            req.checkInDate = "2026-09-01"
            req.checkOutDate = "2026-09-02"
            req.numberOfNights = 1
            req.totalPrice = 0.0d
            req.paymentMethod = PaymentMethod.CREDIT_CARD
            req.confirmed = false
        when:
            bookingService.createBooking(req, uow)
        then:
            thrown(Exception)
    }
}
