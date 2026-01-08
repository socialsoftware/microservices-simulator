package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaCategory;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaCategoryDto extends CategoryDto {
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