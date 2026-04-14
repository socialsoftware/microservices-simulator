package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;

public interface CustomerFactory {
    Customer createCustomer(Integer aggregateId, CustomerDto customerDto);
    Customer createCustomerFromExisting(Customer existingCustomer);
    CustomerDto createCustomerDto(Customer customer);
}
