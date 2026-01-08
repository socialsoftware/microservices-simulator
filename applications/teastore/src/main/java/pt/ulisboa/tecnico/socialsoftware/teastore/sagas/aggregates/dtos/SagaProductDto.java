package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaProduct;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaProductDto extends ProductDto {
private SagaState sagaState;

public SagaProductDto(Product product) {
super((Product) product);
this.sagaState = ((SagaProduct)product).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}