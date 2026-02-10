package com.generated.abstractions.microservices.customer.aggregate;

import com.generated.abstractions.shared.dtos.CustomerDto;

public interface CustomerFactory {
    Customer createCustomer(Integer aggregateId, CustomerDto customerDto);
    Customer createCustomerFromExisting(Customer existingCustomer);
    CustomerDto createCustomerDto(Customer customer);
}
