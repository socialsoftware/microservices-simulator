package com.generated.abstractions.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.generated.abstractions.coordination.functionalities.CustomerFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import com.generated.abstractions.shared.dtos.CustomerDto;
import com.generated.abstractions.coordination.webapi.requestDtos.CreateCustomerRequestDto;

@RestController
public class CustomerController {
    @Autowired
    private CustomerFunctionalities customerFunctionalities;

    @PostMapping("/customers/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto createCustomer(@RequestBody CreateCustomerRequestDto createRequest) {
        return customerFunctionalities.createCustomer(createRequest);
    }

    @GetMapping("/customers/{customerAggregateId}")
    public CustomerDto getCustomerById(@PathVariable Integer customerAggregateId) {
        return customerFunctionalities.getCustomerById(customerAggregateId);
    }

    @PutMapping("/customers")
    public CustomerDto updateCustomer(@RequestBody CustomerDto customerDto) {
        return customerFunctionalities.updateCustomer(customerDto);
    }

    @DeleteMapping("/customers/{customerAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomer(@PathVariable Integer customerAggregateId) {
        customerFunctionalities.deleteCustomer(customerAggregateId);
    }

    @GetMapping("/customers")
    public List<CustomerDto> getAllCustomers() {
        return customerFunctionalities.getAllCustomers();
    }
}
