package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderTripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderFromStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderToStationDto;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.OrderUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;


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
            orderDto.setBoughtDate(createRequest.getBoughtDate());
            orderDto.setTravelDate(createRequest.getTravelDate());
            orderDto.setTravelTime(createRequest.getTravelTime());
            orderDto.setCoachNumber(createRequest.getCoachNumber());
            orderDto.setSeatClass(createRequest.getSeatClass() != null ? createRequest.getSeatClass().name() : null);
            orderDto.setSeatNumber(createRequest.getSeatNumber());
            orderDto.setPrice(createRequest.getPrice());
            orderDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            if (createRequest.getUser() != null) {
                OrderUserDto userDto = new OrderUserDto();
                userDto.setAggregateId(createRequest.getUser().getAggregateId());
                userDto.setVersion(createRequest.getUser().getVersion());
                userDto.setState(createRequest.getUser().getState() != null ? createRequest.getUser().getState().name() : null);
                orderDto.setUser(userDto);
            }
            if (createRequest.getContacts() != null) {
                OrderContactsDto contactsDto = new OrderContactsDto();
                contactsDto.setAggregateId(createRequest.getContacts().getAggregateId());
                contactsDto.setVersion(createRequest.getContacts().getVersion());
                contactsDto.setState(createRequest.getContacts().getState() != null ? createRequest.getContacts().getState().name() : null);
                orderDto.setContacts(contactsDto);
            }
            if (createRequest.getTrip() != null) {
                OrderTripDto tripDto = new OrderTripDto();
                tripDto.setAggregateId(createRequest.getTrip().getAggregateId());
                tripDto.setVersion(createRequest.getTrip().getVersion());
                tripDto.setState(createRequest.getTrip().getState() != null ? createRequest.getTrip().getState().name() : null);
                orderDto.setTrip(tripDto);
            }
            if (createRequest.getTrain() != null) {
                OrderTrainDto trainDto = new OrderTrainDto();
                trainDto.setAggregateId(createRequest.getTrain().getAggregateId());
                trainDto.setVersion(createRequest.getTrain().getVersion());
                trainDto.setState(createRequest.getTrain().getState() != null ? createRequest.getTrain().getState().name() : null);
                orderDto.setTrain(trainDto);
            }
            if (createRequest.getFromStation() != null) {
                OrderFromStationDto fromStationDto = new OrderFromStationDto();
                fromStationDto.setAggregateId(createRequest.getFromStation().getAggregateId());
                fromStationDto.setVersion(createRequest.getFromStation().getVersion());
                fromStationDto.setState(createRequest.getFromStation().getState() != null ? createRequest.getFromStation().getState().name() : null);
                orderDto.setFromStation(fromStationDto);
            }
            if (createRequest.getToStation() != null) {
                OrderToStationDto toStationDto = new OrderToStationDto();
                toStationDto.setAggregateId(createRequest.getToStation().getAggregateId());
                toStationDto.setVersion(createRequest.getToStation().getVersion());
                toStationDto.setState(createRequest.getToStation().getState() != null ? createRequest.getToStation().getState().name() : null);
                orderDto.setToStation(toStationDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Order order = orderFactory.createOrder(aggregateId, orderDto);
            unitOfWorkService.registerChanged(order, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating order: " + e.getMessage());
        }
    }

    public OrderDto getOrderById(Integer id, UnitOfWork unitOfWork) {
        try {
            Order order = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return orderFactory.createOrderDto(order);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving order: " + e.getMessage());
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
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving order: " + e.getMessage());
        }
    }

    public OrderDto updateOrder(OrderDto orderDto, UnitOfWork unitOfWork) {
        try {
            Integer id = orderDto.getAggregateId();
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            if (orderDto.getBoughtDate() != null) {
                newOrder.setBoughtDate(orderDto.getBoughtDate());
            }
            if (orderDto.getTravelDate() != null) {
                newOrder.setTravelDate(orderDto.getTravelDate());
            }
            if (orderDto.getTravelTime() != null) {
                newOrder.setTravelTime(orderDto.getTravelTime());
            }
            if (orderDto.getCoachNumber() != null) {
                newOrder.setCoachNumber(orderDto.getCoachNumber());
            }
            if (orderDto.getSeatClass() != null) {
                newOrder.setSeatClass(SeatClass.valueOf(orderDto.getSeatClass()));
            }
            if (orderDto.getSeatNumber() != null) {
                newOrder.setSeatNumber(orderDto.getSeatNumber());
            }
            if (orderDto.getPrice() != null) {
                newOrder.setPrice(orderDto.getPrice());
            }
            if (orderDto.getStatus() != null) {
                newOrder.setStatus(OrderStatus.valueOf(orderDto.getStatus()));
            }

            unitOfWorkService.registerChanged(newOrder, unitOfWork);            OrderUpdatedEvent event = new OrderUpdatedEvent(newOrder.getAggregateId(), newOrder.getBoughtDate(), newOrder.getTravelDate(), newOrder.getTravelTime(), newOrder.getCoachNumber(), newOrder.getSeatNumber(), newOrder.getPrice());
            event.setPublisherAggregateVersion(newOrder.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return orderFactory.createOrderDto(newOrder);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating order: " + e.getMessage());
        }
    }

    public void deleteOrder(Integer id, UnitOfWork unitOfWork) {
        try {
            Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
            newOrder.remove();
            unitOfWorkService.registerChanged(newOrder, unitOfWork);            unitOfWorkService.registerEvent(new OrderDeletedEvent(newOrder.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting order: " + e.getMessage());
        }
    }








}