package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;

public interface BookFactory {
    Book createBook(Integer aggregateId, BookDto bookDto);
    Book createBookFromExisting(Book existingBook);
    BookDto createBookDto(Book book);
}
