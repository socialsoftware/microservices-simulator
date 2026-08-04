package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface StationRepository extends JpaRepository<Station, Integer> {

}