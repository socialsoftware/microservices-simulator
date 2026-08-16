package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas.SagaAuthor;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaAuthorDto extends AuthorDto {
@Convert(converter = SagaStateConverter.class)
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