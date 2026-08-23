package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private OrderFactory orderFactory;

    private final OrderRepository orderRepository;
    private final OrderCustomRepository orderCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public OrderService(UnitOfWorkService unitOfWorkService,
                        OrderRepository orderRepository,
                        OrderCustomRepository orderCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.orderRepository = orderRepository;
        this.orderCustomRepository = orderCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderDto getOrderById(Integer orderAggregateId, UnitOfWork unitOfWork) {
        return orderFactory.createOrderDto(
                (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<OrderDto> getOrders(UnitOfWork unitOfWork) {
        return loadAll(orderCustomRepository.findAllLatestActive(), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<OrderDto> getOrdersByAccount(Integer userAggregateId, UnitOfWork unitOfWork) {
        return loadAll(orderCustomRepository.findAllLatestActiveByUser(userAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Integer getLeftTicketCount(Integer tripAggregateId, LocalDate travelDate, SeatClass seatClass,
                                      Integer capacity, UnitOfWork unitOfWork) {
        List<OrderDto> heldSeats = loadAll(
                orderCustomRepository.findAllLatestActiveHoldingSeatOn(tripAggregateId, travelDate, seatClass),
                unitOfWork);
        return capacity - heldSeats.size();
    }

    private List<OrderDto> loadAll(List<Order> orders, UnitOfWork unitOfWork) {
        List<OrderDto> orderDtos = new ArrayList<>();
        for (Order order : orders) {
            orderDtos.add(orderFactory.createOrderDto(
                    (Order) unitOfWorkService.aggregateLoadAndRegisterRead(order.getAggregateId(), unitOfWork)));
        }
        return orderDtos;
    }
}
