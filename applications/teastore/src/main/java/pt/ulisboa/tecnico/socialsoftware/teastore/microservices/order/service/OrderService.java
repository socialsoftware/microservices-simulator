package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish.OrderUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.events.publish.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.enums.Object;

@Service
public class OrderService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final OrderRepository orderRepository;

    @Autowired
    private OrderFactory orderFactory;

    public OrderService(UnitOfWorkService unitOfWorkService, OrderRepository orderRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.orderRepository = orderRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderDto createOrder(OrderUser user, OrderDto orderDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Order order = orderFactory.createOrder(aggregateId, user, orderDto);
        unitOfWorkService.registerChanged(order, unitOfWork);
        return orderFactory.createOrderDto(order);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderDto getOrderById(Integer aggregateId, UnitOfWork unitOfWork) {
        return orderFactory.createOrderDto((Order) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderDto updateOrder(OrderDto orderDto, UnitOfWork unitOfWork) {
        Integer aggregateId = orderDto.getAggregateId();
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
        newOrder.setTime(orderDto.getTime());
        newOrder.setTotalPriceInCents(orderDto.getTotalPriceInCents());
        newOrder.setAddressName(orderDto.getAddressName());
        newOrder.setAddress1(orderDto.getAddress1());
        newOrder.setAddress2(orderDto.getAddress2());
        newOrder.setCreditCardCompany(orderDto.getCreditCardCompany());
        newOrder.setCreditCardNumber(orderDto.getCreditCardNumber());
        newOrder.setCreditCardExpiryDate(orderDto.getCreditCardExpiryDate());
        unitOfWorkService.registerChanged(newOrder, unitOfWork);
        unitOfWorkService.registerEvent(new OrderUpdatedEvent(newOrder.getAggregateId(), newOrder.getTime(), newOrder.getTotalPriceInCents(), newOrder.getAddressName(), newOrder.getAddress1(), newOrder.getAddress2(), newOrder.getCreditCardCompany(), newOrder.getCreditCardNumber(), newOrder.getCreditCardExpiryDate()), unitOfWork);
        return orderFactory.createOrderDto(newOrder);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteOrder(Integer aggregateId, UnitOfWork unitOfWork) {
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
        newOrder.remove();
        unitOfWorkService.registerChanged(newOrder, unitOfWork);
        unitOfWorkService.registerEvent(new OrderDeletedEvent(newOrder.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<OrderDto> searchOrders(String time, String addressName, String address1, String address2, String creditCardCompany, String creditCardNumber, String creditCardExpiryDate, Integer userAggregateId, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = orderRepository.findAll().stream()
                .filter(entity -> {
                    if (time != null) {
                        if (!entity.getTime().equals(time)) {
                            return false;
                        }
                    }
                    if (addressName != null) {
                        if (!entity.getAddressName().equals(addressName)) {
                            return false;
                        }
                    }
                    if (address1 != null) {
                        if (!entity.getAddress1().equals(address1)) {
                            return false;
                        }
                    }
                    if (address2 != null) {
                        if (!entity.getAddress2().equals(address2)) {
                            return false;
                        }
                    }
                    if (creditCardCompany != null) {
                        if (!entity.getCreditCardCompany().equals(creditCardCompany)) {
                            return false;
                        }
                    }
                    if (creditCardNumber != null) {
                        if (!entity.getCreditCardNumber().equals(creditCardNumber)) {
                            return false;
                        }
                    }
                    if (creditCardExpiryDate != null) {
                        if (!entity.getCreditCardExpiryDate().equals(creditCardExpiryDate)) {
                            return false;
                        }
                    }
                    if (userAggregateId != null) {
                        if (!entity.getUser().getUserAggregateId().equals(userAggregateId)) {
                            return false;
                        }
                                            }
                    return true;
                })
                .map(Order::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(orderFactory::createOrderDto)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Object cancelOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement cancelOrder method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order findByOrderId(Integer orderAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement findByOrderId method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Object findByUserAggregateId(Integer userAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement findByUserAggregateId method
        return null;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Order updateOrderStatus(Integer orderAggregateId, String status, UnitOfWork unitOfWork) {
        // TODO: Implement updateOrderStatus method
        return null;
    }

}
