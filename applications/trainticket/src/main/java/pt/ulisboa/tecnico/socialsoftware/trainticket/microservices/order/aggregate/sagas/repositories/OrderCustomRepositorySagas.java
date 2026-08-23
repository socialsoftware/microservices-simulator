package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;

import java.time.LocalDate;
import java.util.List;

@Service
@Profile("sagas")
public class OrderCustomRepositorySagas implements OrderCustomRepository {
    @Autowired
    private OrderRepository orderRepository;

    @Override
    public List<Order> findAllLatestActive() {
        return orderRepository.findAllLatestActive();
    }

    @Override
    public List<Order> findAllLatestActiveByUser(Integer userAggregateId) {
        return orderRepository.findAllLatestActiveByUser(userAggregateId);
    }

    @Override
    public List<Order> findAllLatestActiveHoldingSeatOn(Integer tripAggregateId, LocalDate travelDate,
                                                        SeatClass seatClass) {
        return orderRepository.findAllLatestActiveHoldingSeatOn(tripAggregateId, travelDate, seatClass);
    }
}
