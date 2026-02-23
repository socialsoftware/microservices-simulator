package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderCustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderCustomerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderCustomerUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderProductDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderProductRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos.CreateOrderRequestDto;


@Service
@Transactional
public class OrderService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderFactory orderFactory;

    public OrderService() {}

    public OrderDto createOrder(CreateOrderRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            OrderDto orderDto = new OrderDto();
            orderDto.setTotalAmount(createRequest.getTotalAmount());
            orderDto.setOrderDate(createRequest.getOrderDate());
            if (createRequest.getCustomer() != null) {
                OrderCustomerDto customerDto = new OrderCustomerDto();
                customerDto.setAggregateId(createRequest.getCustomer().getAggregateId());
                customerDto.setVersion(createRequest.getCustomer().getVersion());
                customerDto.setState(createRequest.getCustomer().getState());
                orderDto.setCustomer(customerDto);
            }
            if (createRequest.getProducts() != null) {
                orderDto.setProducts(createRequest.getProducts().stream().map(srcDto -> {
                    OrderProductDto projDto = new OrderProductDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toSet()));
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Order order = orderFactory.createOrder(aggregateId, orderDto);
            unitOfWorkService.registerChanged(order, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error creating order: " + e.getMessage());
        }
    }

    public OrderDto getOrderById(Integer id, UnitOfWork unitOfWork) {
        try {
            Order order = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving order: " + e.getMessage());
        }
    }

    public List<OrderDto> getAllOrders(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = orderRepository.findAll().stream()
                .map(Order::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(orderFactory::createOrderDto)
                .collect(Collectors.toList());
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving order: " + e.getMessage());
        }
    }

    public OrderDto updateOrder(OrderDto orderDto, UnitOfWork unitOfWork) {
        try {
            Integer id = orderDto.getAggregateId();
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            if (orderDto.getTotalAmount() != null) {
                newOrder.setTotalAmount(orderDto.getTotalAmount());
            }
            if (orderDto.getOrderDate() != null) {
                newOrder.setOrderDate(orderDto.getOrderDate());
            }

            unitOfWorkService.registerChanged(newOrder, unitOfWork);            OrderUpdatedEvent event = new OrderUpdatedEvent(newOrder.getAggregateId(), newOrder.getTotalAmount(), newOrder.getOrderDate());
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return orderFactory.createOrderDto(newOrder);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error updating order: " + e.getMessage());
        }
    }

    public void deleteOrder(Integer id, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            newOrder.remove();
            unitOfWorkService.registerChanged(newOrder, unitOfWork);            unitOfWorkService.registerEvent(new OrderDeletedEvent(newOrder.getAggregateId()), unitOfWork);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error deleting order: " + e.getMessage());
        }
    }

    public OrderProductDto addOrderProduct(Integer orderId, Integer productAggregateId, OrderProductDto OrderProductDto, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            OrderProduct element = new OrderProduct(OrderProductDto);
            newOrder.getProducts().add(element);
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            return OrderProductDto;
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error adding OrderProduct: " + e.getMessage());
        }
    }

    public List<OrderProductDto> addOrderProducts(Integer orderId, List<OrderProductDto> OrderProductDtos, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            OrderProductDtos.forEach(dto -> {
                OrderProduct element = new OrderProduct(dto);
                newOrder.getProducts().add(element);
            });
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            return OrderProductDtos;
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error adding OrderProducts: " + e.getMessage());
        }
    }

    public OrderProductDto getOrderProduct(Integer orderId, Integer productAggregateId, UnitOfWork unitOfWork) {
        try {
            Order order = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            OrderProduct element = order.getProducts().stream()
                .filter(item -> item.getProductAggregateId() != null &&
                               item.getProductAggregateId().equals(productAggregateId))
                .findFirst()
                .orElseThrow(() -> new AdvancedException("OrderProduct not found"));
            return element.buildDto();
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving OrderProduct: " + e.getMessage());
        }
    }

    public void removeOrderProduct(Integer orderId, Integer productAggregateId, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            newOrder.getProducts().removeIf(item ->
                item.getProductAggregateId() != null &&
                item.getProductAggregateId().equals(productAggregateId)
            );
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            OrderProductRemovedEvent event = new OrderProductRemovedEvent(orderId, productAggregateId);
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error removing OrderProduct: " + e.getMessage());
        }
    }

    public OrderProductDto updateOrderProduct(Integer orderId, Integer productAggregateId, OrderProductDto OrderProductDto, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            OrderProduct element = newOrder.getProducts().stream()
                .filter(item -> item.getProductAggregateId() != null &&
                               item.getProductAggregateId().equals(productAggregateId))
                .findFirst()
                .orElseThrow(() -> new AdvancedException("OrderProduct not found"));

            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            OrderProductUpdatedEvent event = new OrderProductUpdatedEvent(orderId, element.getProductAggregateId(), element.getProductVersion(), element.getProductName(), element.getProductPrice());
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error updating OrderProduct: " + e.getMessage());
        }
    }






}