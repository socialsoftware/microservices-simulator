package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas.SagaRoom;

@Repository
public interface RoomCustomRepositorySagas extends JpaRepository<SagaRoom, Integer> {
}