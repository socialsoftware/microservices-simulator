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
class RoomCrudSpec extends Specification {

    @Autowired RoomService roomService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createRoom(String number, String description, Double price, RoomStatus status) {
        def uow = unitOfWorkService.createUnitOfWork("room-create-$number")
        def req = new CreateRoomRequestDto()
        req.roomNumber = number
        req.description = description
        req.pricePerNight = price
        req.amenities = [] as Set
        req.status = status
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "createRoom persists and getRoomById reads back"() {
        given:
            def tag = "${System.nanoTime()}"
        when:
            def room = createRoom("R-$tag", "Deluxe", 150.0d, RoomStatus.AVAILABLE)
        then:
            def uow = unitOfWorkService.createUnitOfWork("room-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uow)
            found.roomNumber == "R-$tag"
            found.description == "Deluxe"
            found.pricePerNight == 150.0d
            found.status == "AVAILABLE"
    }

    def "pricePerNight round-trips as Double"() {
        given:
            def tag = "${System.nanoTime()}"
        when:
            def room = createRoom("P-$tag", "Suite", 99.95d, RoomStatus.AVAILABLE)
        then:
            def uow = unitOfWorkService.createUnitOfWork("room-price-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uow)
            found.pricePerNight == 99.95d
    }

    def "getAllRooms returns created rooms"() {
        given:
            def tag = "${System.nanoTime()}"
            createRoom("LA-$tag", "Single", 80.0d, RoomStatus.AVAILABLE)
            createRoom("LB-$tag", "Double", 120.0d, RoomStatus.AVAILABLE)
        when:
            def uow = unitOfWorkService.createUnitOfWork("room-list-$tag")
            def all = roomService.getAllRooms(uow)
        then:
            all.any { it.roomNumber == "LA-$tag" }
            all.any { it.roomNumber == "LB-$tag" }
    }

    def "updateRoom mutates description and price"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom("U-$tag", "Original desc", 100.0d, RoomStatus.AVAILABLE)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("room-upd-get-$tag")
            def existing = roomService.getRoomById(room.aggregateId, uowGet)
            existing.description = "Renovated"
            existing.pricePerNight = 175.5d
            def uowPut = unitOfWorkService.createUnitOfWork("room-upd-put-$tag")
            roomService.updateRoom(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("room-upd-read-$tag")
            def reloaded = roomService.getRoomById(room.aggregateId, uowR)
            reloaded.description == "Renovated"
            reloaded.pricePerNight == 175.5d
    }

    def "status persists and is readable as string"() {
        given:
            def tag = "${System.nanoTime()}"
        when:
            def room = createRoom("S-$tag", "OOS room", 90.0d, RoomStatus.OUT_OF_SERVICE)
        then:
            def uow = unitOfWorkService.createUnitOfWork("room-status-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uow)
            found.status == "OUT_OF_SERVICE"
    }

    def "deleteRoom hides the row from subsequent reads"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom("D-$tag", "To delete", 50.0d, RoomStatus.AVAILABLE)
        when:
            def uowDel = unitOfWorkService.createUnitOfWork("room-del-$tag")
            roomService.deleteRoom(room.aggregateId, uowDel)
            unitOfWorkService.commit(uowDel)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("room-del-read-$tag")
            try {
                roomService.getRoomById(room.aggregateId, uowR)
                assert false : "expected to throw"
            } catch (Exception ignored) {}
    }
}
