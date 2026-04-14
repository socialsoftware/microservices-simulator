package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderUserDto;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class OrderService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderFactory orderFactory;

    @Autowired
    private OrderServiceExtension extension;

    @Autowired
    private ApplicationContext applicationContext;

    public OrderService() {}

    public OrderDto createOrder(CreateOrderRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            OrderDto orderDto = new OrderDto();
            orderDto.setTotalInCents(createRequest.getTotalInCents());
            orderDto.setItemCount(createRequest.getItemCount());
            orderDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            orderDto.setPlacedAt(createRequest.getPlacedAt());
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                OrderUserDto userDto = new OrderUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setUsername(refSourceDto.getUsername());
                userDto.setEmail(refSourceDto.getEmail());
                userDto.setShippingAddress(refSourceDto.getShippingAddress());
                orderDto.setUser(userDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Order order = orderFactory.createOrder(aggregateId, orderDto);
            unitOfWorkService.registerChanged(order, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating order: " + e.getMessage());
        }
    }

    public OrderDto getOrderById(Integer id, UnitOfWork unitOfWork) {
        try {
            Order order = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving order: " + e.getMessage());
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
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving order: " + e.getMessage());
        }
    }

    public OrderDto updateOrder(OrderDto orderDto, UnitOfWork unitOfWork) {
        try {
            Integer id = orderDto.getAggregateId();
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            if (orderDto.getTotalInCents() != null) {
                newOrder.setTotalInCents(orderDto.getTotalInCents());
            }
            if (orderDto.getItemCount() != null) {
                newOrder.setItemCount(orderDto.getItemCount());
            }
            if (orderDto.getStatus() != null) {
                newOrder.setStatus(OrderStatus.valueOf(orderDto.getStatus()));
            }
            if (orderDto.getPlacedAt() != null) {
                newOrder.setPlacedAt(orderDto.getPlacedAt());
            }

            unitOfWorkService.registerChanged(newOrder, unitOfWork);            OrderUpdatedEvent event = new OrderUpdatedEvent(newOrder.getAggregateId(), newOrder.getTotalInCents(), newOrder.getItemCount(), newOrder.getPlacedAt());
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return orderFactory.createOrderDto(newOrder);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating order: " + e.getMessage());
        }
    }

    public void deleteOrder(Integer id, UnitOfWork unitOfWork) {
        try {
            InvoiceRepository invoiceRepositoryRef = applicationContext.getBean(InvoiceRepository.class);
            boolean hasInvoiceReferences = invoiceRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != Order.AggregateState.DELETED)
                .anyMatch(s -> s.getOrder() != null && id.equals(s.getOrder().getOrderAggregateId()));
            if (hasInvoiceReferences) {
                throw new EcommerceException("Cannot delete order that has invoices");
            }
            PaymentRepository paymentRepositoryRef = applicationContext.getBean(PaymentRepository.class);
            boolean hasPaymentReferences = paymentRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != Order.AggregateState.DELETED)
                .anyMatch(s -> s.getOrder() != null && id.equals(s.getOrder().getOrderAggregateId()));
            if (hasPaymentReferences) {
                throw new EcommerceException("Cannot delete order that has payments");
            }
            ShippingRepository shippingRepositoryRef = applicationContext.getBean(ShippingRepository.class);
            boolean hasShippingReferences = shippingRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != Order.AggregateState.DELETED)
                .anyMatch(s -> s.getOrder() != null && id.equals(s.getOrder().getOrderAggregateId()));
            if (hasShippingReferences) {
                throw new EcommerceException("Cannot delete order that has shipping");
            }
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            newOrder.remove();
            unitOfWorkService.registerChanged(newOrder, unitOfWork);            unitOfWorkService.registerEvent(new OrderDeletedEvent(newOrder.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting order: " + e.getMessage());
        }
    }




    public Order handleUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userEmail, String shippingAddress, UnitOfWork unitOfWork) {
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
                    userEmail,
                    shippingAddress
            ),
            unitOfWork
        );

            return newOrder;
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error handling UserUpdatedEvent order: " + e.getMessage());
        }
    }

    @Transactional
    public OrderDto placeOrder(User user, Double totalInCents, Integer itemCount, String placedAt, UnitOfWork unitOfWork) {
        try {
        OrderDto dto = new OrderDto();
        dto.setUser(user);
        dto.setTotalInCents(totalInCents);
        dto.setItemCount(itemCount);
        dto.setStatus("PENDING");
        dto.setPlacedAt(placedAt);
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Order order = orderFactory.createOrder(aggregateId, dto);
        unitOfWorkService.registerChanged(order, unitOfWork);
        return orderFactory.createOrderDto(order);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error in placeOrder Order: " + e.getMessage());
        }
    }

    @Transactional
    public void markPaid(Integer orderId, UnitOfWork unitOfWork) {
        try {
        Order orderOld = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
        Order order = orderFactory.createOrderFromExisting(orderOld);
        order.setStatus(OrderStatus.PAID);
        unitOfWorkService.registerChanged(order, unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error in markPaid Order: " + e.getMessage());
        }
    }

    @Transactional
    public void markShipped(Integer orderId, UnitOfWork unitOfWork) {
        try {
        Order orderOld = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
        Order order = orderFactory.createOrderFromExisting(orderOld);
        order.setStatus(OrderStatus.SHIPPED);
        unitOfWorkService.registerChanged(order, unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error in markShipped Order: " + e.getMessage());
        }
    }

    @Transactional
    public void markDelivered(Integer orderId, UnitOfWork unitOfWork) {
        try {
        Order orderOld = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
        Order order = orderFactory.createOrderFromExisting(orderOld);
        order.setStatus(OrderStatus.DELIVERED);
        unitOfWorkService.registerChanged(order, unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error in markDelivered Order: " + e.getMessage());
        }
    }

    @Transactional
    public void cancelOrder(Integer orderId, UnitOfWork unitOfWork) {
        try {
        Order orderOld = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderId, unitOfWork);
        Order order = orderFactory.createOrderFromExisting(orderOld);
        order.setStatus(OrderStatus.CANCELLED);
        unitOfWorkService.registerChanged(order, unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error in cancelOrder Order: " + e.getMessage());
        }
    }


}