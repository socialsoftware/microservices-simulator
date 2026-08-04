package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface RouteRepository extends JpaRepository<Route, Integer> {

}