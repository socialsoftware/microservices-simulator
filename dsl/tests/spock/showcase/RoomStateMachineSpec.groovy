package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class RoomStateMachineSpec extends Specification {

    @Autowired RoomService roomService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createAvailableRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("rsm-create-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "RSM-$tag"
        req.description = "State machine room"
        req.pricePerNight = 100.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "a fresh room starts in AVAILABLE status"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createAvailableRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("rsm-fresh-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uow)
        then:
            found.status == "AVAILABLE"
    }

    def "reserve transitions status to RESERVED"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createAvailableRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("rsm-reserve-$tag")
            roomService.reserve(room.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("rsm-reserve-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
            found.status == "RESERVED"
    }

    def "checkIn transitions status to OCCUPIED"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createAvailableRoom(tag)
            def uowRes = unitOfWorkService.createUnitOfWork("rsm-ci-reserve-$tag")
            roomService.reserve(room.aggregateId, uowRes)
            unitOfWorkService.commit(uowRes)
        when:
            def uow = unitOfWorkService.createUnitOfWork("rsm-ci-$tag")
            roomService.checkIn(room.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("rsm-ci-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
            found.status == "OCCUPIED"
    }

    def "checkOut transitions status to AVAILABLE"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createAvailableRoom(tag)
            def uowRes = unitOfWorkService.createUnitOfWork("rsm-co-reserve-$tag")
            roomService.reserve(room.aggregateId, uowRes)
            unitOfWorkService.commit(uowRes)
            def uowCi = unitOfWorkService.createUnitOfWork("rsm-co-ci-$tag")
            roomService.checkIn(room.aggregateId, uowCi)
            unitOfWorkService.commit(uowCi)
        when:
            def uow = unitOfWorkService.createUnitOfWork("rsm-co-$tag")
            roomService.checkOut(room.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("rsm-co-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
            found.status == "AVAILABLE"
    }

    def "release after reserve returns status to AVAILABLE"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createAvailableRoom(tag)
            def uowRes = unitOfWorkService.createUnitOfWork("rsm-rel-reserve-$tag")
            roomService.reserve(room.aggregateId, uowRes)
            unitOfWorkService.commit(uowRes)
        when:
            def uow = unitOfWorkService.createUnitOfWork("rsm-rel-$tag")
            roomService.release(room.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("rsm-rel-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
            found.status == "AVAILABLE"
    }

    def "retire transitions status to OUT_OF_SERVICE"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createAvailableRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("rsm-ret-$tag")
            roomService.retire(room.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("rsm-ret-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
            found.status == "OUT_OF_SERVICE"
    }
}
