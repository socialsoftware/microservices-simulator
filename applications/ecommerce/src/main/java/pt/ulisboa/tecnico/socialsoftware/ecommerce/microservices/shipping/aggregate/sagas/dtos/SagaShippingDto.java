package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.SagaShipping;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaShippingDto extends ShippingDto {
private SagaState sagaState;

public SagaShippingDto(Shipping shipping) {
super((Shipping) shipping);
this.sagaState = ((SagaShipping)shipping).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}