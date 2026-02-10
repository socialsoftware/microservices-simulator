package com.generated.abstractions.microservices.customer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.generated.abstractions.microservices.customer.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.generated.abstractions.shared.dtos.CustomerDto;
import com.generated.abstractions.shared.dtos.AddressDto;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import com.generated.abstractions.microservices.customer.events.publish.CustomerDeletedEvent;
import com.generated.abstractions.microservices.customer.events.publish.CustomerUpdatedEvent;
import com.generated.abstractions.microservices.exception.AbstractionsException;
import com.generated.abstractions.coordination.webapi.requestDtos.CreateCustomerRequestDto;


@Service
@Transactional
public class CustomerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerFactory customerFactory;

    public CustomerService() {}

    public CustomerDto createCustomer(CreateCustomerRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            CustomerDto customerDto = new CustomerDto();
            customerDto.setName(createRequest.getName());
            customerDto.setEmail(createRequest.getEmail());
            customerDto.setAddress(createRequest.getAddress());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Customer customer = customerFactory.createCustomer(aggregateId, customerDto);
            unitOfWorkService.registerChanged(customer, unitOfWork);
            return customerFactory.createCustomerDto(customer);
        } catch (Exception e) {
            throw new AbstractionsException("Error creating customer: " + e.getMessage());
        }
    }

    public CustomerDto getCustomerById(Integer id, UnitOfWork unitOfWork) {
        try {
            Customer customer = (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return customerFactory.createCustomerDto(customer);
        } catch (AbstractionsException e) {
            throw e;
        } catch (Exception e) {
            throw new AbstractionsException("Error retrieving customer: " + e.getMessage());
        }
    }

    public List<CustomerDto> getAllCustomers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = customerRepository.findAll().stream()
                .map(Customer::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(customerFactory::createCustomerDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AbstractionsException("Error retrieving all customers: " + e.getMessage());
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

            unitOfWorkService.registerChanged(newCustomer, unitOfWork);
            CustomerUpdatedEvent event = new CustomerUpdatedEvent(newCustomer.getAggregateId(), newCustomer.getName(), newCustomer.getEmail());
            event.setPublisherAggregateVersion(newCustomer.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return customerFactory.createCustomerDto(newCustomer);
        } catch (AbstractionsException e) {
            throw e;
        } catch (Exception e) {
            throw new AbstractionsException("Error updating customer: " + e.getMessage());
        }
    }

    public void deleteCustomer(Integer id, UnitOfWork unitOfWork) {
        try {
            Customer oldCustomer = (Customer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Customer newCustomer = customerFactory.createCustomerFromExisting(oldCustomer);
            newCustomer.remove();
            unitOfWorkService.registerChanged(newCustomer, unitOfWork);
            unitOfWorkService.registerEvent(new CustomerDeletedEvent(newCustomer.getAggregateId()), unitOfWork);
        } catch (AbstractionsException e) {
            throw e;
        } catch (Exception e) {
            throw new AbstractionsException("Error deleting customer: " + e.getMessage());
        }
    }








}