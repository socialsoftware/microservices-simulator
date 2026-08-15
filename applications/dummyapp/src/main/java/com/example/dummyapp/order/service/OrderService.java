package com.example.dummyapp.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import com.example.dummyapp.events.ItemRenamedEvent;
import com.example.dummyapp.order.aggregate.Order;
import com.example.dummyapp.order.aggregate.OrderDto;
import com.example.dummyapp.order.aggregate.OrderRepository;

import java.util.logging.Logger;

@Service
public class OrderService implements OrderServiceApi {

    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    @Autowired
    private UnitOfWorkService uow;

    @Autowired
    private OrderRepository repository;

    private final UnsupportedEventRegistrar unsupportedEventRegistrar = new UnsupportedEventRegistrar();

    @Transactional
    public OrderDto getOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        logger.info("Getting order " + orderAggregateId);
        Order order = (Order) uow.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
        uow.registerEvent(new ItemRenamedEvent(orderAggregateId, orderAggregateId, "renamed-from-order"), unitOfWork);
        return new OrderDto(order);
    }

    // Negative verifier fixtures: these source shapes must never become exact event consequences.
    public void emitThroughWrongReceiver(Integer orderAggregateId, UnitOfWork unitOfWork) {
        unsupportedEventRegistrar.registerEvent(
                new ItemRenamedEvent(orderAggregateId, orderAggregateId, "wrong-receiver"), unitOfWork);
    }

    public void emitWithWrongUnitOfWork(Integer orderAggregateId, UnitOfWork unitOfWork) {
        uow.registerEvent(new ItemRenamedEvent(orderAggregateId, orderAggregateId, "wrong-unit-of-work"), null);
    }

    @Transactional
    public OrderDto placeOrder(OrderDto orderDto, UnitOfWork unitOfWork) {
        logger.info("Placing order");
        Order order = new Order(null, "PLACED");
        uow.registerChanged(order, unitOfWork);
        return new OrderDto(order);
    }

    @Transactional
    public void cancelOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        logger.info("Cancelling order " + orderAggregateId);
        Order order = (Order) uow.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
        order.setStatus("CANCELLED");
        uow.registerChanged(order, unitOfWork);
    }
}

final class UnsupportedEventRegistrar {
    void registerEvent(Event event, UnitOfWork unitOfWork) {
        // Source-only verifier fixture. This is intentionally not a simulator UnitOfWorkService.
    }
}
