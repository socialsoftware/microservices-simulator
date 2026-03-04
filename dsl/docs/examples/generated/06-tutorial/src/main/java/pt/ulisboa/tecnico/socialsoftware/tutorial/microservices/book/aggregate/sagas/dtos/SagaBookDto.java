package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas.SagaBook;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaBookDto extends BookDto {
private SagaState sagaState;

public SagaBookDto(Book book) {
super((Book) book);
this.sagaState = ((SagaBook)book).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}