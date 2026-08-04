package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.sagas.SagaPriceConfig;

@Repository
public interface PriceConfigCustomRepositorySagas extends JpaRepository<SagaPriceConfig, Integer> {
}