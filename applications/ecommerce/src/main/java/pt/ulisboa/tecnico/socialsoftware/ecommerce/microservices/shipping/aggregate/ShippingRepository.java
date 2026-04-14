package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ShippingRepository extends JpaRepository<Shipping, Integer> {

}