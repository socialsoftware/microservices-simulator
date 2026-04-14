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

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ConcurrentSpec extends Specification {

    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("cc-user-$tag")
        def req = new CreateUserRequestDto()
        req.username = "cc-user-$tag"
        req.email = "cc-${tag}@example.com"
        req.loyaltyPoints = 0
        req.tier = MembershipTier.BRONZE
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("cc-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "CC-$tag"
        req.description = "Concurrent room"
        req.pricePerNight = 110.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "two concurrent reserve calls against the same room — at least one completes"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = seedRoom(tag)
            def pool = Executors.newFixedThreadPool(2)
        when:
            def tasks = (1..2).collect { i ->
                { ->
                    try {
                        def uow = unitOfWorkService.createUnitOfWork("cc-reserve-${tag}-${i}")
                        roomService.reserve(room.aggregateId, uow)
                        unitOfWorkService.commit(uow)
                        return "ok"
                    } catch (Exception e) {
                        return "fail:" + e.class.simpleName
                    }
                } as Callable<String>
            }
            def futures = pool.invokeAll(tasks)
            def results = futures.collect { it.get(30, TimeUnit.SECONDS) }
            pool.shutdown()
        then: "at least one of the two reserve attempts succeeded"
            results.count { it == "ok" } >= 1
    }

    def "rapid sequential awardLoyaltyPoints calls produce a consistent final state"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
        when:
            (1..5).each { i ->
                def uow = unitOfWorkService.createUnitOfWork("cc-award-${tag}-${i}")
                userService.awardLoyaltyPoints(user.aggregateId, i * 10, uow)
                unitOfWorkService.commit(uow)
            }
        then:
            def uowR = unitOfWorkService.createUnitOfWork("cc-award-read-$tag")
            def found = userService.getUserById(user.aggregateId, uowR)
            found.loyaltyPoints == 50
    }

    def "concurrent createBooking against the same user/room — both creations can succeed"() {
        given:
            def tag = "${System.nanoTime()}"
            def user = seedUser(tag)
            def room = seedRoom(tag)
            def pool = Executors.newFixedThreadPool(2)
        when:
            def tasks = (1..2).collect { i ->
                { ->
                    try {
                        def uow = unitOfWorkService.createUnitOfWork("cc-bk-${tag}-${i}")
                        def req = new CreateBookingRequestDto()
                        req.user = user
                        req.room = room
                        req.checkInDate = "2026-09-0${i}"
                        req.checkOutDate = "2026-09-1${i}"
                        req.numberOfNights = 2
                        req.totalPrice = 220.0d
                        req.paymentMethod = PaymentMethod.CREDIT_CARD
                        req.confirmed = false
                        def dto = bookingService.createBooking(req, uow)
                        unitOfWorkService.commit(uow)
                        return dto.aggregateId
                    } catch (Exception ignored) {
                        return null
                    }
                } as Callable<Integer>
            }
            def futures = pool.invokeAll(tasks)
            def ids = futures.collect { it.get(30, TimeUnit.SECONDS) }.findAll { it != null }
            pool.shutdown()
        then: "at least one booking was created, and any successful IDs are distinct"
            ids.size() >= 1
            ids.toSet().size() == ids.size()
    }
}
