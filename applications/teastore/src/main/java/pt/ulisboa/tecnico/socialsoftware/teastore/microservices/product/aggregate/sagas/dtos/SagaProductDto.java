package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.SagaProduct;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaProductDto extends ProductDto {
@Convert(converter = SagaStateConverter.class)
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