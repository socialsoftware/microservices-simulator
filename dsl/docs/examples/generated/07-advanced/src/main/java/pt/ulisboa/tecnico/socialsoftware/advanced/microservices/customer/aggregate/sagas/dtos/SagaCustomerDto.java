package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.sagas.SagaCustomer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaCustomerDto extends CustomerDto {
@Convert(converter = SagaStateConverter.class)
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