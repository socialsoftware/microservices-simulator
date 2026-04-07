package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceOrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.InvoiceUserDto;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.enums.InvoiceStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.InvoiceDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.InvoiceUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.webapi.requestDtos.CreateInvoiceRequestDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;


@Service
@Transactional(noRollbackFor = EcommerceException.class)
public class InvoiceService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceFactory invoiceFactory;

    @Autowired
    private InvoiceServiceExtension extension;

    public InvoiceService() {}

    public InvoiceDto createInvoice(CreateInvoiceRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            InvoiceDto invoiceDto = new InvoiceDto();
            invoiceDto.setInvoiceNumber(createRequest.getInvoiceNumber());
            invoiceDto.setAmountInCents(createRequest.getAmountInCents());
            invoiceDto.setIssuedAt(createRequest.getIssuedAt());
            invoiceDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            if (createRequest.getOrder() != null) {
                Order refSource = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getOrder().getAggregateId(), unitOfWork);
                OrderDto refSourceDto = new OrderDto(refSource);
                InvoiceOrderDto orderDto = new InvoiceOrderDto();
                orderDto.setAggregateId(refSourceDto.getAggregateId());
                orderDto.setVersion(refSourceDto.getVersion());
                orderDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                orderDto.setTotalInCents(refSourceDto.getTotalInCents());
                invoiceDto.setOrder(orderDto);
            }
            if (createRequest.getUser() != null) {
                User refSource = (User) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getUser().getAggregateId(), unitOfWork);
                UserDto refSourceDto = new UserDto(refSource);
                InvoiceUserDto userDto = new InvoiceUserDto();
                userDto.setAggregateId(refSourceDto.getAggregateId());
                userDto.setVersion(refSourceDto.getVersion());
                userDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                userDto.setUsername(refSourceDto.getUsername());
                userDto.setEmail(refSourceDto.getEmail());
                invoiceDto.setUser(userDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Invoice invoice = invoiceFactory.createInvoice(aggregateId, invoiceDto);
            unitOfWorkService.registerChanged(invoice, unitOfWork);
            return invoiceFactory.createInvoiceDto(invoice);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error creating invoice: " + e.getMessage());
        }
    }

    public InvoiceDto getInvoiceById(Integer id, UnitOfWork unitOfWork) {
        try {
            Invoice invoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return invoiceFactory.createInvoiceDto(invoice);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving invoice: " + e.getMessage());
        }
    }

    public List<InvoiceDto> getAllInvoices(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = invoiceRepository.findAll().stream()
                .map(Invoice::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(invoiceFactory::createInvoiceDto)
                .collect(Collectors.toList());
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error retrieving invoice: " + e.getMessage());
        }
    }

    public InvoiceDto updateInvoice(InvoiceDto invoiceDto, UnitOfWork unitOfWork) {
        try {
            Integer id = invoiceDto.getAggregateId();
            Invoice oldInvoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Invoice newInvoice = invoiceFactory.createInvoiceFromExisting(oldInvoice);
            if (invoiceDto.getInvoiceNumber() != null) {
                newInvoice.setInvoiceNumber(invoiceDto.getInvoiceNumber());
            }
            if (invoiceDto.getAmountInCents() != null) {
                newInvoice.setAmountInCents(invoiceDto.getAmountInCents());
            }
            if (invoiceDto.getIssuedAt() != null) {
                newInvoice.setIssuedAt(invoiceDto.getIssuedAt());
            }
            if (invoiceDto.getStatus() != null) {
                newInvoice.setStatus(InvoiceStatus.valueOf(invoiceDto.getStatus()));
            }

            unitOfWorkService.registerChanged(newInvoice, unitOfWork);            InvoiceUpdatedEvent event = new InvoiceUpdatedEvent(newInvoice.getAggregateId(), newInvoice.getInvoiceNumber(), newInvoice.getAmountInCents(), newInvoice.getIssuedAt());
            event.setPublisherAggregateVersion(newInvoice.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return invoiceFactory.createInvoiceDto(newInvoice);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error updating invoice: " + e.getMessage());
        }
    }

    public void deleteInvoice(Integer id, UnitOfWork unitOfWork) {
        try {
            Invoice oldInvoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Invoice newInvoice = invoiceFactory.createInvoiceFromExisting(oldInvoice);
            newInvoice.remove();
            unitOfWorkService.registerChanged(newInvoice, unitOfWork);            unitOfWorkService.registerEvent(new InvoiceDeletedEvent(newInvoice.getAggregateId()), unitOfWork);
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException("Error deleting invoice: " + e.getMessage());
        }
    }




    public void handlePaymentAuthorizedEvent(Integer aggregateId, UnitOfWork unitOfWork) {
    }

    public void handleOrderCancelledEvent(Integer aggregateId, UnitOfWork unitOfWork) {
    }




}