package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.SagaTrip;

@Repository
public interface TripCustomRepositorySagas extends JpaRepository<SagaTrip, Integer> {
}