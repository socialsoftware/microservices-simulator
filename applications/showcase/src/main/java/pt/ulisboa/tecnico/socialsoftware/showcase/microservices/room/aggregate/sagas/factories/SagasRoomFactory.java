package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.Room;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.RoomFactory;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.SagaRoom;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.dtos.SagaRoomDto;

@Service
@Profile("sagas")
public class SagasRoomFactory implements RoomFactory {
    @Override
    public Room createRoom(Integer aggregateId, RoomDto roomDto) {
        return new SagaRoom(aggregateId, roomDto);
    }

    @Override
    public Room createRoomFromExisting(Room existingRoom) {
        return new SagaRoom((SagaRoom) existingRoom);
    }

    @Override
    public RoomDto createRoomDto(Room room) {
        return new SagaRoomDto(room);
    }
}