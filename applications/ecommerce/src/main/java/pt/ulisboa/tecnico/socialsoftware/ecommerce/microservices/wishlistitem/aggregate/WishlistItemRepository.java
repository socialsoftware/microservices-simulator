package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Integer> {

}