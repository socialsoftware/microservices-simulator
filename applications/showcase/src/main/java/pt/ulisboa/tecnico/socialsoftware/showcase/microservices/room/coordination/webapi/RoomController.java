package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.functionalities.RoomFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto;

@RestController
public class RoomController {
    @Autowired
    private RoomFunctionalities roomFunctionalities;

    @PostMapping("/rooms/create")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDto createRoom(@RequestBody CreateRoomRequestDto createRequest) {
        return roomFunctionalities.createRoom(createRequest);
    }

    @GetMapping("/rooms/{roomAggregateId}")
    public RoomDto getRoomById(@PathVariable Integer roomAggregateId) {
        return roomFunctionalities.getRoomById(roomAggregateId);
    }

    @PutMapping("/rooms")
    public RoomDto updateRoom(@RequestBody RoomDto roomDto) {
        return roomFunctionalities.updateRoom(roomDto);
    }

    @DeleteMapping("/rooms/{roomAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Integer roomAggregateId) {
        roomFunctionalities.deleteRoom(roomAggregateId);
    }

    @GetMapping("/rooms")
    public List<RoomDto> getAllRooms() {
        return roomFunctionalities.getAllRooms();
    }

    @PostMapping("/rooms/{roomId}/amenities")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomAmenityDto addRoomAmenitie(@PathVariable Integer roomId, @RequestParam Integer code, @RequestBody RoomAmenityDto amenitieDto) {
        return roomFunctionalities.addRoomAmenitie(roomId, code, amenitieDto);
    }

    @PostMapping("/rooms/{roomId}/amenities/batch")
    public List<RoomAmenityDto> addRoomAmenities(@PathVariable Integer roomId, @RequestBody List<RoomAmenityDto> amenitieDtos) {
        return roomFunctionalities.addRoomAmenities(roomId, amenitieDtos);
    }

    @GetMapping("/rooms/{roomId}/amenities/{code}")
    public RoomAmenityDto getRoomAmenitie(@PathVariable Integer roomId, @PathVariable Integer code) {
        return roomFunctionalities.getRoomAmenitie(roomId, code);
    }

    @PutMapping("/rooms/{roomId}/amenities/{code}")
    public RoomAmenityDto updateRoomAmenitie(@PathVariable Integer roomId, @PathVariable Integer code, @RequestBody RoomAmenityDto amenitieDto) {
        return roomFunctionalities.updateRoomAmenitie(roomId, code, amenitieDto);
    }

    @DeleteMapping("/rooms/{roomId}/amenities/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRoomAmenitie(@PathVariable Integer roomId, @PathVariable Integer code) {
        roomFunctionalities.removeRoomAmenitie(roomId, code);
    }
}
