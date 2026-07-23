package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.SagaCategory;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaCategoryDto extends CategoryDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaCategoryDto(Category category) {
super((Category) category);
this.sagaState = ((SagaCategory)category).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}