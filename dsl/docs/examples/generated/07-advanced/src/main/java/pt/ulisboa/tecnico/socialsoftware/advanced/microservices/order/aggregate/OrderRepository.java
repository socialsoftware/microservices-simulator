package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface OrderRepository extends JpaRepository<Order, Integer> {
    @Query(value = "select e.aggregateId from Order e where e.totalAmount >= :minAmount AND e.state != 'DELETED'")
    Set<Integer> findOrderIdsAboveAmount(Double minAmount);
}