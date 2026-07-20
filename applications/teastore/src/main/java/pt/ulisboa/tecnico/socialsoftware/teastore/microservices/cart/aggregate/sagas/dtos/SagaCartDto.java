package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.aggregate.sagas.SagaCart;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaCartDto extends CartDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaCartDto(Cart cart) {
super((Cart) cart);
this.sagaState = ((SagaCart)cart).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}