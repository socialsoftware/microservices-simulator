package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate;

import java.util.Set;

public interface OrderCustomRepository {
    Set<Integer> findOrderIdsAboveAmount(Double minAmount);
}