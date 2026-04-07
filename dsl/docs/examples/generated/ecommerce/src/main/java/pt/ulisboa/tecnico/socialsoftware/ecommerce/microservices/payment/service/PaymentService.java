package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentOrderDto;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.PaymentStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class PaymentService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentFactory paymentFactory;

    public PaymentService() {}

    public PaymentDto createPayment(CreatePaymentRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setAmountInCents(createRequest.getAmountInCents());
            paymentDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            paymentDto.setAuthorizationCode(createRequest.getAuthorizationCode());
            paymentDto.setPaymentMethod(createRequest.getPaymentMethod());
            if (createRequest.getOrder() != null) {
                Order refSource = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getOrder().getAggregateId(), unitOfWork);
                OrderDto refSourceDto = new OrderDto(refSource);
                PaymentOrderDto orderDto = new PaymentOrderDto();
                orderDto.setAggregateId(refSourceDto.getAggregateId());
                orderDto.setVersion(refSourceDto.getVersion());
                orderDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                orderDto.setTotalInCents(refSourceDto.getTotalInCents());
                orderDto.setStatus(refSourceDto.getStatus());
                paymentDto.setOrder(orderDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Payment payment = paymentFactory.createPayment(aggregateId, paymentDto);
            unitOfWorkService.registerChanged(payment, unitOfWork);
            return paymentFactory.createPaymentDto(payment);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating payment: " + e.getMessage());
        }
    }

    public PaymentDto getPaymentById(Integer id, UnitOfWork unitOfWork) {
        try {
            Payment payment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return paymentFactory.createPaymentDto(payment);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving payment: " + e.getMessage());
        }
    }

    public List<PaymentDto> getAllPayments(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = paymentRepository.findAll().stream()
                .map(Payment::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(paymentFactory::createPaymentDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving payment: " + e.getMessage());
        }
    }

    public PaymentDto updatePayment(PaymentDto paymentDto, UnitOfWork unitOfWork) {
        try {
            Integer id = paymentDto.getAggregateId();
            Payment oldPayment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Payment newPayment = paymentFactory.createPaymentFromExisting(oldPayment);
            if (paymentDto.getAmountInCents() != null) {
                newPayment.setAmountInCents(paymentDto.getAmountInCents());
            }
            if (paymentDto.getStatus() != null) {
                newPayment.setStatus(PaymentStatus.valueOf(paymentDto.getStatus()));
            }
            if (paymentDto.getAuthorizationCode() != null) {
                newPayment.setAuthorizationCode(paymentDto.getAuthorizationCode());
            }
            if (paymentDto.getPaymentMethod() != null) {
                newPayment.setPaymentMethod(paymentDto.getPaymentMethod());
            }

            unitOfWorkService.registerChanged(newPayment, unitOfWork);            PaymentUpdatedEvent event = new PaymentUpdatedEvent(newPayment.getAggregateId(), newPayment.getAmountInCents(), newPayment.getAuthorizationCode(), newPayment.getPaymentMethod());
            event.setPublisherAggregateVersion(newPayment.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return paymentFactory.createPaymentDto(newPayment);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating payment: " + e.getMessage());
        }
    }

    public void deletePayment(Integer id, UnitOfWork unitOfWork) {
        try {
            Payment oldPayment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Payment newPayment = paymentFactory.createPaymentFromExisting(oldPayment);
            newPayment.remove();
            unitOfWorkService.registerChanged(newPayment, unitOfWork);            unitOfWorkService.registerEvent(new PaymentDeletedEvent(newPayment.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting payment: " + e.getMessage());
        }
    }




    public void handleOrderPlacedEvent(Integer aggregateId, UnitOfWork unitOfWork) {
        // TODO: implement business logic for OrderPlacedEvent.
        // This stub was generated because Payment subscribes to OrderPlacedEvent
        // but the event is not a projection lifecycle event (Updated/Deleted).
    }

    public void handleOrderCancelledEvent(Integer aggregateId, UnitOfWork unitOfWork) {
        // TODO: implement business logic for OrderCancelledEvent.
        // This stub was generated because Payment subscribes to OrderCancelledEvent
        // but the event is not a projection lifecycle event (Updated/Deleted).
    }




}