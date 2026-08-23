package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.CONTACTS_BELONG_TO_ACCOUNT;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.ENDPOINTS_ON_TRIP_ROUTE;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.SEAT_CAPACITY_NOT_EXCEEDED;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.SEAT_NUMBER_WITHIN_CAPACITY;

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public OrderDto preserveTicket(OrderDto requestedOrderDto, ContactsDto contactsDto, TripDto tripDto,
                                   RouteDto routeDto, PriceConfigDto priceConfigDto, Integer capacity,
                                   UnitOfWork unitOfWork) {
        // [P3] CONTACTS_BELONG_TO_ACCOUNT over the ContactsDto the saga assembled.
        if (!requestedOrderDto.getUserAggregateId().equals(contactsDto.getUserAggregateId())) {
            throw new TrainticketException(CONTACTS_BELONG_TO_ACCOUNT);
        }

        // [P3] ENDPOINTS_ON_TRIP_ROUTE over the RouteDto the saga assembled.
        RouteStationDto from = routeStationNamed(routeDto, requestedOrderDto.getFromStationName());
        RouteStationDto to = routeStationNamed(routeDto, requestedOrderDto.getToStationName());
        if (from.getSequence() >= to.getSequence()) {
            throw new TrainticketException(ENDPOINTS_ON_TRIP_ROUTE);
        }

        List<Order> seatsHeld = orderCustomRepository.findAllLatestActiveHoldingSeatOn(
                requestedOrderDto.getTripAggregateId(), requestedOrderDto.getTravelDate(),
                requestedOrderDto.getSeatClass());

        // [P3] SEAT_CAPACITY_NOT_EXCEEDED - counted before the booking, so the inequality is strict.
        if (seatsHeld.size() >= capacity) {
            throw new TrainticketException(SEAT_CAPACITY_NOT_EXCEEDED);
        }

        Integer seatNumber = lowestFreeSeatNumber(seatsHeld);

        // [P3] SEAT_NUMBER_WITHIN_CAPACITY - the upper half of the allocator's bound. It cannot fire
        // while the count guard above holds, since a class with a free slot always leaves a free
        // number in [1, capacity]; it is kept because the two rules bound the same allocation.
        if (seatNumber > capacity) {
            throw new TrainticketException(SEAT_NUMBER_WITHIN_CAPACITY);
        }

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Order order = orderFactory.createOrder(aggregateId,
                bookingTerms(requestedOrderDto, contactsDto, tripDto, priceConfigDto, from, to, seatNumber));

        unitOfWorkService.registerChanged(order, unitOfWork);
        return orderFactory.createOrderDto(order);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void payOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        advanceTo(orderAggregateId, OrderStatus.PAID, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void collectTicket(Integer orderAggregateId, UnitOfWork unitOfWork) {
        advanceTo(orderAggregateId, OrderStatus.COLLECTED, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void useTicket(Integer orderAggregateId, UnitOfWork unitOfWork) {
        advanceTo(orderAggregateId, OrderStatus.USED, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderCopy(oldOrder);
        newOrder.cancel(DateHandler.now());

        unitOfWorkService.registerChanged(newOrder, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteOrder(Integer orderAggregateId, UnitOfWork unitOfWork) {
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderCopy(oldOrder);
        newOrder.remove();

        unitOfWorkService.registerChanged(newOrder, unitOfWork);
    }

    // ORDER_STATUS_TRANSITION is P1: the copy carries prev, so verifyInvariants() rejects an edge
    // the state machine does not have when registerChanged commits it.
    private void advanceTo(Integer orderAggregateId, OrderStatus status, UnitOfWork unitOfWork) {
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(orderAggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderCopy(oldOrder);
        newOrder.setStatus(status);

        unitOfWorkService.registerChanged(newOrder, unitOfWork);
    }

    // [P4b] DEPARTURE_TIME_MATCHES_TRIP and PRICE_MATCHES_TARIFF: each term is derived once, here,
    // from the trip, route and tariff the booking saga fetched, and reaches the order as a single
    // value. Nothing re-checks them afterwards because nothing else can set them.
    private OrderDto bookingTerms(OrderDto requestedOrderDto, ContactsDto contactsDto, TripDto tripDto,
                                  PriceConfigDto priceConfigDto, RouteStationDto from, RouteStationDto to,
                                  Integer seatNumber) {
        return new OrderDto(requestedOrderDto.getTripAggregateId(), requestedOrderDto.getContactsAggregateId(),
                requestedOrderDto.getUserAggregateId(), DateHandler.now(), requestedOrderDto.getTravelDate(),
                LocalDateTime.of(requestedOrderDto.getTravelDate(), tripDto.getStartTime()),
                tripDto.getTripNumber(), requestedOrderDto.getFromStationName(),
                requestedOrderDto.getToStationName(), requestedOrderDto.getSeatClass(), seatNumber,
                contactsDto.getName(), contactsDto.getDocumentType(), contactsDto.getDocumentNumber(),
                fareFor(from, to, priceConfigDto, requestedOrderDto.getSeatClass()));
    }

    private static BigDecimal fareFor(RouteStationDto from, RouteStationDto to,
                                      PriceConfigDto priceConfigDto, SeatClass seatClass) {
        BigDecimal rate = seatClass == SeatClass.FIRST_CLASS
                ? priceConfigDto.getFirstClassPriceRate()
                : priceConfigDto.getBasicPriceRate();
        return rate.multiply(
                BigDecimal.valueOf(to.getDistanceFromStart() - from.getDistanceFromStart()));
    }

    private static RouteStationDto routeStationNamed(RouteDto routeDto, String stationName) {
        return routeDto.getRouteStations().stream()
                .filter(routeStation -> routeStation.getStationName().equals(stationName))
                .findFirst()
                .orElseThrow(() -> new TrainticketException(ENDPOINTS_ON_TRIP_ROUTE));
    }

    // SEAT_NUMBER_UNIQUE_PER_DEPARTURE holds by construction: the number handed out is the lowest one
    // no non-cancelled order on this departure already holds, read from Order's own table inside the
    // SERIALIZABLE transaction that then writes the booking.
    private static Integer lowestFreeSeatNumber(List<Order> seatsHeld) {
        Set<Integer> taken = new HashSet<>();
        for (Order order : seatsHeld) {
            taken.add(order.getSeatNumber());
        }
        Integer seatNumber = 1;
        while (taken.contains(seatNumber)) {
            seatNumber++;
        }
        return seatNumber;
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
