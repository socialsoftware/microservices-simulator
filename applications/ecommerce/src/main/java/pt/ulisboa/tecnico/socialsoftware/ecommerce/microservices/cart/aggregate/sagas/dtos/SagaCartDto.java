package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.sagas.SagaCart;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaCartDto extends CartDto {
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