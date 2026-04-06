package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderUserDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.OrderUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.OrderUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;


@Service
@Transactional(noRollbackFor = TeastoreException.class)
public class OrderService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderFactory orderFactory;

    public OrderService() {}

    public OrderDto createOrder(CreateOrderRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            OrderDto orderDto = new OrderDto();
            orderDto.setTime(createRequest.getTime());
            orderDto.setTotalPriceInCents(createRequest.getTotalPriceInCents());
            orderDto.setAddressName(createRequest.getAddressName());
            orderDto.setAddress1(createRequest.getAddress1());
            orderDto.setAddress2(createRequest.getAddress2());
            orderDto.setCreditCardCompany(createRequest.getCreditCardCompany());
            orderDto.setCreditCardNumber(createRequest.getCreditCardNumber());
            orderDto.setCreditCardExpiryDate(createRequest.getCreditCardExpiryDate());
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                OrderUserDto userDto = new OrderUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setUserName(refSourceDto.getUserName());
                userDto.setRealName(refSourceDto.getRealName());
                userDto.setEmail(refSourceDto.getEmail());
                orderDto.setUser(userDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Order order = orderFactory.createOrder(aggregateId, orderDto);
            unitOfWorkService.registerChanged(order, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error creating order: " + e.getMessage());
        }
    }

    public OrderDto getOrderById(Integer id, UnitOfWork unitOfWork) {
        try {
            Order order = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving order: " + e.getMessage());
        }
    }

    public List<OrderDto> getAllOrders(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = orderRepository.findAll().stream()
                .map(Order::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(orderFactory::createOrderDto)
                .collect(Collectors.toList());
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving order: " + e.getMessage());
        }
    }

    public OrderDto updateOrder(OrderDto orderDto, UnitOfWork unitOfWork) {
        try {
            Integer id = orderDto.getAggregateId();
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            if (orderDto.getTime() != null) {
                newOrder.setTime(orderDto.getTime());
            }
            if (orderDto.getTotalPriceInCents() != null) {
                newOrder.setTotalPriceInCents(orderDto.getTotalPriceInCents());
            }
            if (orderDto.getAddressName() != null) {
                newOrder.setAddressName(orderDto.getAddressName());
            }
            if (orderDto.getAddress1() != null) {
                newOrder.setAddress1(orderDto.getAddress1());
            }
            if (orderDto.getAddress2() != null) {
                newOrder.setAddress2(orderDto.getAddress2());
            }
            if (orderDto.getCreditCardCompany() != null) {
                newOrder.setCreditCardCompany(orderDto.getCreditCardCompany());
            }
            if (orderDto.getCreditCardNumber() != null) {
                newOrder.setCreditCardNumber(orderDto.getCreditCardNumber());
            }
            if (orderDto.getCreditCardExpiryDate() != null) {
                newOrder.setCreditCardExpiryDate(orderDto.getCreditCardExpiryDate());
            }

            unitOfWorkService.registerChanged(newOrder, unitOfWork);            OrderUpdatedEvent event = new OrderUpdatedEvent(newOrder.getAggregateId(), newOrder.getTime(), newOrder.getTotalPriceInCents(), newOrder.getAddressName(), newOrder.getAddress1(), newOrder.getAddress2(), newOrder.getCreditCardCompany(), newOrder.getCreditCardNumber(), newOrder.getCreditCardExpiryDate());
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return orderFactory.createOrderDto(newOrder);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error updating order: " + e.getMessage());
        }
    }

    public void deleteOrder(Integer id, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            newOrder.remove();
            unitOfWorkService.registerChanged(newOrder, unitOfWork);            unitOfWorkService.registerEvent(new OrderDeletedEvent(newOrder.getAggregateId()), unitOfWork);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error deleting order: " + e.getMessage());
        }
    }




    public Order handleUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userRealName, String userEmail, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);



            unitOfWorkService.registerChanged(newOrder, unitOfWork);

        unitOfWorkService.registerEvent(
            new OrderUserUpdatedEvent(
                    newOrder.getAggregateId(),
                    userAggregateId,
                    userVersion,
                    userName,
                    userRealName,
                    userEmail
            ),
            unitOfWork
        );

            return newOrder;
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error handling UserUpdatedEvent order: " + e.getMessage());
        }
    }




}