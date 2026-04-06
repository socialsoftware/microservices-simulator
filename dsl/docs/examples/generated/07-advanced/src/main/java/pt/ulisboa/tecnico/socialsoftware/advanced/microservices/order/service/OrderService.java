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
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderItemDto;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.enums.PaymentMethod;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderProductRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderItemRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderItemUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;


@Service
@Transactional(noRollbackFor = AdvancedException.class)
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
            orderDto.setTotalAmount(createRequest.getTotalAmount());
            orderDto.setOrderDate(createRequest.getOrderDate());
            orderDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            orderDto.setPaymentMethod(createRequest.getPaymentMethod() != null ? createRequest.getPaymentMethod().name() : null);
            if (createRequest.getCustomer() != null) {
                Customer refSource = (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getCustomer().getAggregateId(), unitOfWork);
                CustomerDto refSourceDto = new CustomerDto(refSource);
                OrderCustomerDto customerDto = new OrderCustomerDto();
                customerDto.setAggregateId(refSourceDto.getAggregateId());
                customerDto.setVersion(refSourceDto.getVersion());
                customerDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                customerDto.setName(refSourceDto.getName());
                customerDto.setEmail(refSourceDto.getEmail());
                orderDto.setCustomer(customerDto);
            }
            if (createRequest.getProducts() != null) {
                orderDto.setProducts(createRequest.getProducts().stream().map(reqDto -> {
                    Product refItem = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    ProductDto refItemDto = new ProductDto(refItem);
                    OrderProductDto projDto = new OrderProductDto();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);
                    projDto.setName(refItemDto.getName());
                    projDto.setPrice(refItemDto.getPrice());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            orderDto.setItems(createRequest.getItems());

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
            if (orderDto.getStatus() != null) {
                newOrder.setStatus(OrderStatus.valueOf(orderDto.getStatus()));
            }
            if (orderDto.getPaymentMethod() != null) {
                newOrder.setPaymentMethod(PaymentMethod.valueOf(orderDto.getPaymentMethod()));
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

    public OrderItemDto addOrderItem(Integer orderId, Integer key, OrderItemDto OrderItemDto, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            OrderItem element = new OrderItem(OrderItemDto);
            newOrder.getItems().add(element);
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            return OrderItemDto;
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error adding OrderItem: " + e.getMessage());
        }
    }

    public List<OrderItemDto> addOrderItems(Integer orderId, List<OrderItemDto> OrderItemDtos, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            OrderItemDtos.forEach(dto -> {
                OrderItem element = new OrderItem(dto);
                newOrder.getItems().add(element);
            });
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            return OrderItemDtos;
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error adding OrderItems: " + e.getMessage());
        }
    }

    public OrderItemDto getOrderItem(Integer orderId, Integer key, UnitOfWork unitOfWork) {
        try {
            Order order = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            OrderItem element = order.getItems().stream()
                .filter(item -> item.getKey() != null &&
                               item.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new AdvancedException("OrderItem not found"));
            return element.buildDto();
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving OrderItem: " + e.getMessage());
        }
    }

    public void removeOrderItem(Integer orderId, Integer key, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            newOrder.getItems().removeIf(item ->
                item.getKey() != null &&
                item.getKey().equals(key)
            );
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            OrderItemRemovedEvent event = new OrderItemRemovedEvent(orderId, key);
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error removing OrderItem: " + e.getMessage());
        }
    }

    public OrderItemDto updateOrderItem(Integer orderId, Integer key, OrderItemDto OrderItemDto, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            OrderItem element = newOrder.getItems().stream()
                .filter(item -> item.getKey() != null &&
                               item.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new AdvancedException("OrderItem not found"));
            if (OrderItemDto.getProductName() != null) {
                element.setProductName(OrderItemDto.getProductName());
            }
            if (OrderItemDto.getQuantity() != null) {
                element.setQuantity(OrderItemDto.getQuantity());
            }
            if (OrderItemDto.getUnitPrice() != null) {
                element.setUnitPrice(OrderItemDto.getUnitPrice());
            }
            unitOfWorkService.registerChanged(newOrder, unitOfWork);
            OrderItemUpdatedEvent event = new OrderItemUpdatedEvent(orderId, key);
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error updating OrderItem: " + e.getMessage());
        }
    }






}