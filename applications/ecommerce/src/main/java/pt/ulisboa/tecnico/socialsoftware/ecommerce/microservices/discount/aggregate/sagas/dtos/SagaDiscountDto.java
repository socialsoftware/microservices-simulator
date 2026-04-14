package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.Discount;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.Discount;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.SagaDiscount;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaDiscountDto extends DiscountDto {
private SagaState sagaState;

public SagaDiscountDto(Discount discount) {
super((Discount) discount);
this.sagaState = ((SagaDiscount)discount).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}