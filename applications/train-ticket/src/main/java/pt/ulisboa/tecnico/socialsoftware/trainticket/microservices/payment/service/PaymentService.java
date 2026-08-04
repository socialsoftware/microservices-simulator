package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentOrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PaymentUserDto;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.PaymentType;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.PaymentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.PaymentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto;


@Service
@Transactional
public class PaymentService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentFactory paymentFactory;

    public PaymentService() {}

    public PaymentDto createPayment(CreatePaymentRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setAmount(createRequest.getAmount());
            paymentDto.setType(createRequest.getType() != null ? createRequest.getType().name() : null);
            paymentDto.setPaymentDate(createRequest.getPaymentDate());
            if (createRequest.getOrder() != null) {
                PaymentOrderDto orderDto = new PaymentOrderDto();
                orderDto.setAggregateId(createRequest.getOrder().getAggregateId());
                orderDto.setVersion(createRequest.getOrder().getVersion());
                orderDto.setState(createRequest.getOrder().getState() != null ? createRequest.getOrder().getState().name() : null);
                paymentDto.setOrder(orderDto);
            }
            if (createRequest.getUser() != null) {
                PaymentUserDto userDto = new PaymentUserDto();
                userDto.setAggregateId(createRequest.getUser().getAggregateId());
                userDto.setVersion(createRequest.getUser().getVersion());
                userDto.setState(createRequest.getUser().getState() != null ? createRequest.getUser().getState().name() : null);
                paymentDto.setUser(userDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Payment payment = paymentFactory.createPayment(aggregateId, paymentDto);
            unitOfWorkService.registerChanged(payment, unitOfWork);
            return paymentFactory.createPaymentDto(payment);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating payment: " + e.getMessage());
        }
    }

    public PaymentDto getPaymentById(Integer id, UnitOfWork unitOfWork) {
        try {
            Payment payment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return paymentFactory.createPaymentDto(payment);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving payment: " + e.getMessage());
        }
    }

    public List<PaymentDto> getAllPayments(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = paymentRepository.findAll().stream()
                .map(Payment::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(paymentFactory::createPaymentDto)
                .collect(Collectors.toList());
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving payment: " + e.getMessage());
        }
    }

    public PaymentDto updatePayment(PaymentDto paymentDto, UnitOfWork unitOfWork) {
        try {
            Integer id = paymentDto.getAggregateId();
            Payment oldPayment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Payment newPayment = paymentFactory.createPaymentFromExisting(oldPayment);
            if (paymentDto.getAmount() != null) {
                newPayment.setAmount(paymentDto.getAmount());
            }
            if (paymentDto.getType() != null) {
                newPayment.setType(PaymentType.valueOf(paymentDto.getType()));
            }
            if (paymentDto.getPaymentDate() != null) {
                newPayment.setPaymentDate(paymentDto.getPaymentDate());
            }

            unitOfWorkService.registerChanged(newPayment, unitOfWork);            PaymentUpdatedEvent event = new PaymentUpdatedEvent(newPayment.getAggregateId(), newPayment.getAmount(), newPayment.getPaymentDate());
            event.setPublisherAggregateVersion(newPayment.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return paymentFactory.createPaymentDto(newPayment);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating payment: " + e.getMessage());
        }
    }

    public void deletePayment(Integer id, UnitOfWork unitOfWork) {
        try {
            Payment oldPayment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Payment newPayment = paymentFactory.createPaymentFromExisting(oldPayment);
            newPayment.remove();
            unitOfWorkService.registerChanged(newPayment, unitOfWork);            unitOfWorkService.registerEvent(new PaymentDeletedEvent(newPayment.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting payment: " + e.getMessage());
        }
    }








}