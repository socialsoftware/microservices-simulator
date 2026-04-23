package pt.ulisboa.tecnico.socialsoftware.showcase

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class RoomAmenitiesSpec extends Specification {

    @Autowired RoomService roomService
    @Autowired UnitOfWorkService unitOfWorkService

    private def createRoom(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("amen-create-$tag")
        def req = new CreateRoomRequestDto()
        req.roomNumber = "AMEN-$tag"
        req.description = "Amenities room"
        req.pricePerNight = 100.0d
        req.amenities = [] as Set
        req.status = RoomStatus.AVAILABLE
        def dto = roomService.createRoom(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private RoomAmenityDto amenity(Integer code, String name, String description) {
        def a = new RoomAmenityDto()
        a.code = code
        a.name = name
        a.description = description
        return a
    }

    def "addRoomAmenity + getRoomAmenity round-trip"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("amen-add-$tag")
            roomService.addRoomAmenity(room.aggregateId, 1, amenity(1, "WiFi", "Free high-speed"), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("amen-add-read-$tag")
            def found = roomService.getRoomAmenity(room.aggregateId, 1, uowR)
            found.name == "WiFi"
            found.description == "Free high-speed"
    }

    def "updateRoomAmenity changes the name"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom(tag)
            def uowAdd = unitOfWorkService.createUnitOfWork("amen-upd-add-$tag")
            roomService.addRoomAmenity(room.aggregateId, 2, amenity(2, "TV", "Flat screen"), uowAdd)
            unitOfWorkService.commit(uowAdd)
        when:
            def uow = unitOfWorkService.createUnitOfWork("amen-upd-$tag")
            def patch = new RoomAmenityDto()
            patch.name = "Smart TV"
            roomService.updateRoomAmenity(room.aggregateId, 2, patch, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("amen-upd-read-$tag")
            def found = roomService.getRoomAmenity(room.aggregateId, 2, uowR)
            found.name == "Smart TV"
    }

    def "removeRoomAmenity removes it so subsequent get throws"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom(tag)
            def uowAdd = unitOfWorkService.createUnitOfWork("amen-rm-add-$tag")
            roomService.addRoomAmenity(room.aggregateId, 3, amenity(3, "Minibar", "Stocked"), uowAdd)
            unitOfWorkService.commit(uowAdd)
        when:
            def uow = unitOfWorkService.createUnitOfWork("amen-rm-$tag")
            roomService.removeRoomAmenity(room.aggregateId, 3, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("amen-rm-read-$tag")
            try {
                roomService.getRoomAmenity(room.aggregateId, 3, uowR)
                assert false : "expected to throw"
            } catch (Exception ignored) {}
    }

    def "multiple amenities visible via getRoomById.amenities"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom(tag)
            def uowA = unitOfWorkService.createUnitOfWork("amen-multi-a-$tag")
            roomService.addRoomAmenity(room.aggregateId, 10, amenity(10, "Safe", "In closet"), uowA)
            unitOfWorkService.commit(uowA)
            def uowB = unitOfWorkService.createUnitOfWork("amen-multi-b-$tag")
            roomService.addRoomAmenity(room.aggregateId, 11, amenity(11, "Balcony", "Ocean view"), uowB)
            unitOfWorkService.commit(uowB)
        when:
            def uowR = unitOfWorkService.createUnitOfWork("amen-multi-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
        then:
            found.amenities != null
            found.amenities.size() >= 2
            found.amenities.any { it.code == 10 && it.name == "Safe" }
            found.amenities.any { it.code == 11 && it.name == "Balcony" }
    }

    def "addRoomAmenity then getRoomById.amenities exposes the item"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom(tag)
        when:
            def uow = unitOfWorkService.createUnitOfWork("amen-visible-$tag")
            roomService.addRoomAmenity(room.aggregateId, 20, amenity(20, "Pool access", "24/7"), uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("amen-visible-read-$tag")
            def found = roomService.getRoomById(room.aggregateId, uowR)
            found.amenities.any { it.code == 20 && it.name == "Pool access" }
    }

    def "renameAmenity uses find-where to locate and rename an amenity by code"() {
        given:
            def tag = "${System.nanoTime()}"
            def room = createRoom(tag)
            def uowAdd = unitOfWorkService.createUnitOfWork("amen-rename-add-$tag")
            roomService.addRoomAmenity(room.aggregateId, 30, amenity(30, "OldName", "desc"), uowAdd)
            unitOfWorkService.commit(uowAdd)
        when:
            def uow = unitOfWorkService.createUnitOfWork("amen-rename-$tag")
            roomService.renameAmenity(room.aggregateId, 30, "NewName", uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("amen-rename-read-$tag")
            def found = roomService.getRoomAmenity(room.aggregateId, 30, uowR)
            found.name == "NewName"
    }
}
