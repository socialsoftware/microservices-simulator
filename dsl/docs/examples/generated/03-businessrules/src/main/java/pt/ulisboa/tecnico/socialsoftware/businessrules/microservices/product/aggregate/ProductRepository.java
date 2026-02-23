package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate;

import java.util.Optional;
import java.util.Set;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Query(value = "SELECT e FROM Product e WHERE e.active = true")
    List<Product> findByActiveTrue();
    @Query(value = "SELECT e FROM Product e WHERE e.sku = :sku")
    Optional<Product> findBySku(String sku);
    @Query(value = "select p.aggregateId from Product p where p.price <= :maxPrice AND p.state != 'DELETED'")
    Set<Integer> findAffordableProductIds(Double maxPrice);
}