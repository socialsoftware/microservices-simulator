package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.sagas.SagaWishlistItem;

@Repository
public interface WishlistItemCustomRepositorySagas extends JpaRepository<SagaWishlistItem, Integer> {
}