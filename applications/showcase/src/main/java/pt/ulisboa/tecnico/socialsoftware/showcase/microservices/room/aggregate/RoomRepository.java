package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface RoomRepository extends JpaRepository<Room, Integer> {

}