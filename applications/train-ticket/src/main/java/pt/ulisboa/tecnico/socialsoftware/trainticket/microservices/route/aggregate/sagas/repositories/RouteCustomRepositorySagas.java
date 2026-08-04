package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.SagaRoute;

@Repository
public interface RouteCustomRepositorySagas extends JpaRepository<SagaRoute, Integer> {
}