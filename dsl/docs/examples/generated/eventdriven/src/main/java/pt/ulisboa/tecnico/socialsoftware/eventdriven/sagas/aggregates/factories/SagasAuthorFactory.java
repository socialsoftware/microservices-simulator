package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.Author;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.AuthorFactory;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.SagaAuthor;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.dtos.SagaAuthorDto;

@Service
@Profile("sagas")
public class SagasAuthorFactory implements AuthorFactory {
    @Override
    public Author createAuthor(Integer aggregateId, AuthorDto authorDto) {
        return new SagaAuthor(aggregateId, authorDto);
    }

    @Override
    public Author createAuthorFromExisting(Author existingAuthor) {
        return new SagaAuthor((SagaAuthor) existingAuthor);
    }

    @Override
    public AuthorDto createAuthorDto(Author author) {
        return new SagaAuthorDto(author);
    }
}