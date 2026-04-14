package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;

public interface RoomFactory {
    Room createRoom(Integer aggregateId, RoomDto roomDto);
    Room createRoomFromExisting(Room existingRoom);
    RoomDto createRoomDto(Room room);
}
