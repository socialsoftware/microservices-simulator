package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.SagaCustomer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaCustomerDto extends CustomerDto {
private SagaState sagaState;

public SagaCustomerDto(Customer customer) {
super((Customer) customer);
this.sagaState = ((SagaCustomer)customer).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}