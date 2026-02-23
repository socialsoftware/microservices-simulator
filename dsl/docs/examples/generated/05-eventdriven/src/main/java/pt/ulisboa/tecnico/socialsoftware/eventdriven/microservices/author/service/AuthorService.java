package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish.AuthorDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish.AuthorUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception.EventdrivenException;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreateAuthorRequestDto;


@Service
@Transactional
public class AuthorService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private AuthorFactory authorFactory;

    public AuthorService() {}

    public AuthorDto createAuthor(CreateAuthorRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            AuthorDto authorDto = new AuthorDto();
            authorDto.setName(createRequest.getName());
            authorDto.setBio(createRequest.getBio());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Author author = authorFactory.createAuthor(aggregateId, authorDto);
            unitOfWorkService.registerChanged(author, unitOfWork);
            return authorFactory.createAuthorDto(author);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error creating author: " + e.getMessage());
        }
    }

    public AuthorDto getAuthorById(Integer id, UnitOfWork unitOfWork) {
        try {
            Author author = (Author) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return authorFactory.createAuthorDto(author);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error retrieving author: " + e.getMessage());
        }
    }

    public List<AuthorDto> getAllAuthors(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = authorRepository.findAll().stream()
                .map(Author::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Author) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(authorFactory::createAuthorDto)
                .collect(Collectors.toList());
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error retrieving author: " + e.getMessage());
        }
    }

    public AuthorDto updateAuthor(AuthorDto authorDto, UnitOfWork unitOfWork) {
        try {
            Integer id = authorDto.getAggregateId();
            Author oldAuthor = (Author) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Author newAuthor = authorFactory.createAuthorFromExisting(oldAuthor);
            if (authorDto.getName() != null) {
                newAuthor.setName(authorDto.getName());
            }
            if (authorDto.getBio() != null) {
                newAuthor.setBio(authorDto.getBio());
            }

            unitOfWorkService.registerChanged(newAuthor, unitOfWork);            AuthorUpdatedEvent event = new AuthorUpdatedEvent(newAuthor.getAggregateId(), newAuthor.getName(), newAuthor.getBio());
            event.setPublisherAggregateVersion(newAuthor.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return authorFactory.createAuthorDto(newAuthor);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error updating author: " + e.getMessage());
        }
    }

    public void deleteAuthor(Integer id, UnitOfWork unitOfWork) {
        try {
            Author oldAuthor = (Author) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Author newAuthor = authorFactory.createAuthorFromExisting(oldAuthor);
            newAuthor.remove();
            unitOfWorkService.registerChanged(newAuthor, unitOfWork);            unitOfWorkService.registerEvent(new AuthorDeletedEvent(newAuthor.getAggregateId()), unitOfWork);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error deleting author: " + e.getMessage());
        }
    }








}