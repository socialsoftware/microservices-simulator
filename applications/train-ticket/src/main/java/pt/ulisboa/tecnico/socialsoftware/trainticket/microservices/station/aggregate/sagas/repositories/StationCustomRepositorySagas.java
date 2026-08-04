package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas.SagaStation;

@Repository
public interface StationCustomRepositorySagas extends JpaRepository<SagaStation, Integer> {
}