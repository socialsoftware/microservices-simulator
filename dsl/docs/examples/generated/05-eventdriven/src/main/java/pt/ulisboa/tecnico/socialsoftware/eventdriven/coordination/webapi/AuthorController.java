package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.functionalities.AuthorFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreateAuthorRequestDto;

@RestController
public class AuthorController {
    @Autowired
    private AuthorFunctionalities authorFunctionalities;

    @PostMapping("/authors/create")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorDto createAuthor(@RequestBody CreateAuthorRequestDto createRequest) {
        return authorFunctionalities.createAuthor(createRequest);
    }

    @GetMapping("/authors/{authorAggregateId}")
    public AuthorDto getAuthorById(@PathVariable Integer authorAggregateId) {
        return authorFunctionalities.getAuthorById(authorAggregateId);
    }

    @PutMapping("/authors")
    public AuthorDto updateAuthor(@RequestBody AuthorDto authorDto) {
        return authorFunctionalities.updateAuthor(authorDto);
    }

    @DeleteMapping("/authors/{authorAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAuthor(@PathVariable Integer authorAggregateId) {
        authorFunctionalities.deleteAuthor(authorAggregateId);
    }

    @GetMapping("/authors")
    public List<AuthorDto> getAllAuthors() {
        return authorFunctionalities.getAllAuthors();
    }
}
