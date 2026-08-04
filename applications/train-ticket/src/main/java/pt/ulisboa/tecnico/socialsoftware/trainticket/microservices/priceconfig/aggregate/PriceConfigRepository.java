package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface PriceConfigRepository extends JpaRepository<PriceConfig, Integer> {

}