package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.SagaAuthor;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaAuthorDto extends AuthorDto {
private SagaState sagaState;

public SagaAuthorDto(Author author) {
super((Author) author);
this.sagaState = ((SagaAuthor)author).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}