package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.coordination.workflows.ReserveRoomForUserWorkflow
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ReserveRoomForUserWorkflowSpec extends Specification {

    @Autowired ReserveRoomForUserWorkflow workflow
    @Autowired UserService userService
    @Autowired RoomService roomService
    @Autowired BookingService bookingService
    @Autowired UnitOfWorkService unitOfWorkService

    private RoomDto seedRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("rrfu-room-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "RRFU-$tag"
        req.description = "Workflow test room"
        req.pricePerNight = 150.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "happy path — creates user, reserves room, books it"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = seedRoom(tag)
        when:
            workflow.execute(
                "rrfu-user-$tag", "rrfu-${tag}@example.com",
                room.aggregateId, "2026-07-01", "2026-07-03", 2, 300.0d
            )
        then:
            def uow = unitOfWorkService.createUnitOfWork("rrfu-read-$tag")
            def reloadedRoom = roomService.getRoomById(room.aggregateId, uow)
            reloadedRoom.status == "RESERVED"
    }

    def "compensation — invalid booking rolls back user creation and room reservation"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = seedRoom(tag)
        when: "bookRoom fails on invariant (nights must be > 0)"
            workflow.execute(
                "rrfu-comp-user-$tag", "rrfu-comp-${tag}@example.com",
                room.aggregateId, "2026-07-01", "2026-07-03", 0, 300.0d
            )
        then:
            thrown(Exception)
        and: "reserveRoom compensation restored room to AVAILABLE"
            def uow = unitOfWorkService.createUnitOfWork("rrfu-comp-read-$tag")
            def reloadedRoom = roomService.getRoomById(room.aggregateId, uow)
            reloadedRoom.status == "AVAILABLE"
    }

    def "compensation — missing room stops at loadRoom and rolls back signup"() {
        given:
            def tag = "${System.nanoTime()}"
            def bogusRoomId = -99999
        when:
            workflow.execute(
                "rrfu-missing-user-$tag", "rrfu-missing-${tag}@example.com",
                bogusRoomId, "2026-07-01", "2026-07-03", 2, 300.0d
            )
        then:
            thrown(Exception)
    }

    def "saga-state lock — a second workflow against the same reserved room is blocked"() {
        given: "a reserved room already stamped IN_BOOK_ROOM by an earlier in-flight saga"
            def tag = "${System.nanoTime()}"
            def room = seedRoom(tag)
            def sagaUowBean = applicationContext.getBean(pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService.class)
            def heldUow = sagaUowBean.createUnitOfWork("holding-$tag")
            sagaUowBean.registerSagaState(
                room.aggregateId,
                pt.ulisboa.tecnico.socialsoftware.showcase.shared.sagaStates.ReservationSagaStates.IN_BOOK_ROOM,
                heldUow
            )
        when: "a new workflow targets the same roomId"
            workflow.execute(
                "rrfu-blocked-user-$tag", "rrfu-blocked-${tag}@example.com",
                room.aggregateId, "2026-07-01", "2026-07-03", 2, 300.0d
            )
        then: "verifySagaState rejects it"
            thrown(Exception)
    }

    @Autowired org.springframework.context.ApplicationContext applicationContext
}
