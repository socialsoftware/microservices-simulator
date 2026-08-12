package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface CartRepository extends JpaRepository<Cart, Integer> {

}