package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.CustomerFactory;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.SagaCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.dtos.SagaCustomerDto;

@Service
@Profile("sagas")
public class SagasCustomerFactory implements CustomerFactory {
    @Override
    public Customer createCustomer(Integer aggregateId, CustomerDto customerDto) {
        return new SagaCustomer(aggregateId, customerDto);
    }

    @Override
    public Customer createCustomerFromExisting(Customer existingCustomer) {
        return new SagaCustomer((SagaCustomer) existingCustomer);
    }

    @Override
    public CustomerDto createCustomerDto(Customer customer) {
        return new SagaCustomerDto(customer);
    }
}