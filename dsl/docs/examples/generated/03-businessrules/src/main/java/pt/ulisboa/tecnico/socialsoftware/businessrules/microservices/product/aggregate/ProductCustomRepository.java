package pt.ulisboa.tecnico.socialsoftware.businessrules.microservices.product.aggregate;

import java.util.Optional;
import java.util.List;
import java.util.Set;

public interface ProductCustomRepository {
    List<Product> findByActiveTrue();
    Optional<Product> findBySku(String sku);
    Set<Integer> findAffordableProductIds(Double maxPrice);
}