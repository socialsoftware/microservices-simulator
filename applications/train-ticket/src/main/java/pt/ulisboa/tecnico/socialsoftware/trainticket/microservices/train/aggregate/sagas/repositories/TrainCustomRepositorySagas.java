package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.SagaTrain;

@Repository
public interface TrainCustomRepositorySagas extends JpaRepository<SagaTrain, Integer> {
}