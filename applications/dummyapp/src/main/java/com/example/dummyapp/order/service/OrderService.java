package com.example.dummyapp.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
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

    @Transactional
    public OrderDto getOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        logger.info("Getting order " + orderAggregateId);
        Order order = (Order) uow.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
        return new OrderDto(order);
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
