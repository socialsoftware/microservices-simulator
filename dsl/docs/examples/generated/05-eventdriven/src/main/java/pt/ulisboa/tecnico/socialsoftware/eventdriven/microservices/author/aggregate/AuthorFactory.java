package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;

public interface AuthorFactory {
    Author createAuthor(Integer aggregateId, AuthorDto authorDto);
    Author createAuthorFromExisting(Author existingAuthor);
    AuthorDto createAuthorDto(Author author);
}
