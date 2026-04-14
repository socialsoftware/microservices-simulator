package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.Room;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.Room;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.SagaRoom;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaRoomDto extends RoomDto {
private SagaState sagaState;

public SagaRoomDto(Room room) {
super((Room) room);
this.sagaState = ((SagaRoom)room).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}