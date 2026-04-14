package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.webapi.requestDtos.CreateCustomerRequestDto;


@Service
@Transactional(noRollbackFor = AdvancedException.class)
public class CustomerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerFactory customerFactory;

    @Autowired
    private CustomerServiceExtension extension;

    public CustomerService() {}

    public CustomerDto createCustomer(CreateCustomerRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            CustomerDto customerDto = new CustomerDto();
            customerDto.setName(createRequest.getName());
            customerDto.setEmail(createRequest.getEmail());
            customerDto.setActive(createRequest.getActive());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Customer customer = customerFactory.createCustomer(aggregateId, customerDto);
            unitOfWorkService.registerChanged(customer, unitOfWork);
            return customerFactory.createCustomerDto(customer);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error creating customer: " + e.getMessage());
        }
    }

    public CustomerDto getCustomerById(Integer id, UnitOfWork unitOfWork) {
        try {
            Customer customer = (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return customerFactory.createCustomerDto(customer);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving customer: " + e.getMessage());
        }
    }

    public List<CustomerDto> getAllCustomers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = customerRepository.findAll().stream()
                .map(Customer::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(customerFactory::createCustomerDto)
                .collect(Collectors.toList());
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error retrieving customer: " + e.getMessage());
        }
    }

    public CustomerDto updateCustomer(CustomerDto customerDto, UnitOfWork unitOfWork) {
        try {
            Integer id = customerDto.getAggregateId();
            Customer oldCustomer = (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);
            if (customerDto.getName() != null) {
                newCustomer.setName(customerDto.getName());
            }
            if (customerDto.getEmail() != null) {
                newCustomer.setEmail(customerDto.getEmail());
            }
            newCustomer.setActive(customerDto.getActive());

            unitOfWorkService.registerChanged(newCustomer, unitOfWork);            CustomerUpdatedEvent event = new CustomerUpdatedEvent(newCustomer.getAggregateId(), newCustomer.getName(), newCustomer.getEmail(), newCustomer.getActive());
            event.setPublisherAggregateVersion(newCustomer.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return customerFactory.createCustomerDto(newCustomer);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error updating customer: " + e.getMessage());
        }
    }

    public void deleteCustomer(Integer id, UnitOfWork unitOfWork) {
        try {
            Customer oldCustomer = (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);
            newCustomer.remove();
            unitOfWorkService.registerChanged(newCustomer, unitOfWork);            unitOfWorkService.registerEvent(new CustomerDeletedEvent(newCustomer.getAggregateId()), unitOfWork);
        } catch (AdvancedException e) {
            throw e;
        } catch (Exception e) {
            throw new AdvancedException("Error deleting customer: " + e.getMessage());
        }
    }








}