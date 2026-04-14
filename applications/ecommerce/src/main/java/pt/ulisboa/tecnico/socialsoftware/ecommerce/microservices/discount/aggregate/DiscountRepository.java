package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface DiscountRepository extends JpaRepository<Discount, Integer> {

}