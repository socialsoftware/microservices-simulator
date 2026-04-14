package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.SagaBooking;

@Repository
public interface BookingCustomRepositorySagas extends JpaRepository<SagaBooking, Integer> {
}