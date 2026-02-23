package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.functionalities.BookFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos.CreateBookRequestDto;

@RestController
public class BookController {
    @Autowired
    private BookFunctionalities bookFunctionalities;

    @PostMapping("/books/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto createBook(@RequestBody CreateBookRequestDto createRequest) {
        return bookFunctionalities.createBook(createRequest);
    }

    @GetMapping("/books/{bookAggregateId}")
    public BookDto getBookById(@PathVariable Integer bookAggregateId) {
        return bookFunctionalities.getBookById(bookAggregateId);
    }

    @PutMapping("/books")
    public BookDto updateBook(@RequestBody BookDto bookDto) {
        return bookFunctionalities.updateBook(bookDto);
    }

    @DeleteMapping("/books/{bookAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Integer bookAggregateId) {
        bookFunctionalities.deleteBook(bookAggregateId);
    }

    @GetMapping("/books")
    public List<BookDto> getAllBooks() {
        return bookFunctionalities.getAllBooks();
    }
}
