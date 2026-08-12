package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ProductRepository extends JpaRepository<Product, Integer> {

}