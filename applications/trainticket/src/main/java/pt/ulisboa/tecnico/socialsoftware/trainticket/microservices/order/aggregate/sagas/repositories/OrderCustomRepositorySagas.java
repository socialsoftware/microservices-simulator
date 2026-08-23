package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;

@Service
@Profile("sagas")
public class OrderCustomRepositorySagas implements OrderCustomRepository {
    @Autowired
    private OrderRepository orderRepository;
}
