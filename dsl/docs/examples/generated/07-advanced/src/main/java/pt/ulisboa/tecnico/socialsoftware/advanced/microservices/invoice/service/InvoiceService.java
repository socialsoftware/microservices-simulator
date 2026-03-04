package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceOrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceCustomerDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.InvoiceDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.InvoiceUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.webapi.requestDtos.CreateInvoiceRequestDto;


@Service
@Transactional
public class InvoiceService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceFactory invoiceFactory;

    public InvoiceService() {}

    public InvoiceDto createInvoice(CreateInvoiceRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            InvoiceDto invoiceDto = new InvoiceDto();
            invoiceDto.setTotalAmount(createRequest.getTotalAmount());
            invoiceDto.setIssuedAt(createRequest.getIssuedAt());
            invoiceDto.setPaid(createRequest.getPaid());
            if (createRequest.getOrder() != null) {
                InvoiceOrderDto orderDto = new InvoiceOrderDto();
                orderDto.setAggregateId(createRequest.getOrder().getAggregateId());
                orderDto.setVersion(createRequest.getOrder().getVersion());
                orderDto.setState(createRequest.getOrder().getState() != null ? createRequest.getOrder().getState().name() : null);
                invoiceDto.setOrder(orderDto);
            }
            if (createRequest.getCustomer() != null) {
                InvoiceCustomerDto customerDto = new InvoiceCustomerDto();
                customerDto.setAggregateId(createRequest.getCustomer().getAggregateId());
                customerDto.setVersion(createRequest.getCustomer().getVersion());
                customerDto.setState(createRequest.getCustomer().getState() != null ? createRequest.getCustomer().getState().name() : null);
                invoiceDto.setCustomer(customerDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Invoice invoice = invoiceFactory.createInvoice(aggregateId, invoiceDto);
            unitOfWorkService.registerChanged(invoice, unitOfWork);
            return invoiceFactory.createInvoiceDto(invoice);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error creating invoice: " + e.getMessage());
        }
    }

    public InvoiceDto getInvoiceById(Integer id, UnitOfWork unitOfWork) {
        try {
            Invoice invoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return invoiceFactory.createInvoiceDto(invoice);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving invoice: " + e.getMessage());
        }
    }

    public List<InvoiceDto> getAllInvoices(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = invoiceRepository.findAll().stream()
                .map(Invoice::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(invoiceFactory::createInvoiceDto)
                .collect(Collectors.toList());
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving invoice: " + e.getMessage());
        }
    }

    public InvoiceDto updateInvoice(InvoiceDto invoiceDto, UnitOfWork unitOfWork) {
        try {
            Integer id = invoiceDto.getAggregateId();
            Invoice oldInvoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Invoice newInvoice = invoiceFactory.createInvoiceFromExisting(oldInvoice);
            if (invoiceDto.getTotalAmount() != null) {
                newInvoice.setTotalAmount(invoiceDto.getTotalAmount());
            }
            if (invoiceDto.getIssuedAt() != null) {
                newInvoice.setIssuedAt(invoiceDto.getIssuedAt());
            }
            newInvoice.setPaid(invoiceDto.getPaid());

            unitOfWorkService.registerChanged(newInvoice, unitOfWork);            InvoiceUpdatedEvent event = new InvoiceUpdatedEvent(newInvoice.getAggregateId(), newInvoice.getTotalAmount(), newInvoice.getIssuedAt(), newInvoice.getPaid());
            event.setPublisherAggregateVersion(newInvoice.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return invoiceFactory.createInvoiceDto(newInvoice);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error updating invoice: " + e.getMessage());
        }
    }

    public void deleteInvoice(Integer id, UnitOfWork unitOfWork) {
        try {
            Invoice oldInvoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Invoice newInvoice = invoiceFactory.createInvoiceFromExisting(oldInvoice);
            newInvoice.remove();
            unitOfWorkService.registerChanged(newInvoice, unitOfWork);            unitOfWorkService.registerEvent(new InvoiceDeletedEvent(newInvoice.getAggregateId()), unitOfWork);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error deleting invoice: " + e.getMessage());
        }
    }








}