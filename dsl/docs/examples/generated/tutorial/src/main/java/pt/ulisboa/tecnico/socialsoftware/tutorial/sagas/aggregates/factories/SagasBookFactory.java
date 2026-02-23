package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.Book;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.BookFactory;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.SagaBook;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.dtos.SagaBookDto;

@Service
@Profile("sagas")
public class SagasBookFactory implements BookFactory {
    @Override
    public Book createBook(Integer aggregateId, BookDto bookDto) {
        return new SagaBook(aggregateId, bookDto);
    }

    @Override
    public Book createBookFromExisting(Book existingBook) {
        return new SagaBook((SagaBook) existingBook);
    }

    @Override
    public BookDto createBookDto(Book book) {
        return new SagaBookDto(book);
    }
}