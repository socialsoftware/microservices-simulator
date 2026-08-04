package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TripRepository extends JpaRepository<Trip, Integer> {

}