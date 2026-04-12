package com.example.dummyapp.order.service;

import com.example.dummyapp.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public interface OrderServiceApi {
    OrderDto getOrder(Integer orderAggregateId, UnitOfWork unitOfWork);
    OrderDto placeOrder(OrderDto orderDto, UnitOfWork unitOfWork);
    void cancelOrder(Integer orderAggregateId, UnitOfWork unitOfWork);
}
