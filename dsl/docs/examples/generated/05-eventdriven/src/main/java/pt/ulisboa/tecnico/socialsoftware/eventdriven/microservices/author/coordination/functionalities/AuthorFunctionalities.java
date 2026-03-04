package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception.EventdrivenErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception.EventdrivenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.webapi.requestDtos.CreateAuthorRequestDto;
import java.util.List;

@Service
public class AuthorFunctionalities {
    @Autowired
    private AuthorService authorService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AuthorDto createAuthor(CreateAuthorRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateAuthorFunctionalitySagas createAuthorFunctionalitySagas = new CreateAuthorFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createAuthorFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createAuthorFunctionalitySagas.getCreatedAuthorDto();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AuthorDto getAuthorById(Integer authorAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAuthorByIdFunctionalitySagas getAuthorByIdFunctionalitySagas = new GetAuthorByIdFunctionalitySagas(
                        sagaUnitOfWorkService, authorAggregateId, sagaUnitOfWork, commandGateway);
                getAuthorByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAuthorByIdFunctionalitySagas.getAuthorDto();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public AuthorDto updateAuthor(AuthorDto authorDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(authorDto);
                UpdateAuthorFunctionalitySagas updateAuthorFunctionalitySagas = new UpdateAuthorFunctionalitySagas(
                        sagaUnitOfWorkService, authorDto, sagaUnitOfWork, commandGateway);
                updateAuthorFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateAuthorFunctionalitySagas.getUpdatedAuthorDto();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteAuthor(Integer authorAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteAuthorFunctionalitySagas deleteAuthorFunctionalitySagas = new DeleteAuthorFunctionalitySagas(
                        sagaUnitOfWorkService, authorAggregateId, sagaUnitOfWork, commandGateway);
                deleteAuthorFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<AuthorDto> getAllAuthors() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllAuthorsFunctionalitySagas getAllAuthorsFunctionalitySagas = new GetAllAuthorsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllAuthorsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllAuthorsFunctionalitySagas.getAuthors();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(AuthorDto authorDto) {
        if (authorDto.getName() == null) {
            throw new EventdrivenException(AUTHOR_MISSING_NAME);
        }
        if (authorDto.getBio() == null) {
            throw new EventdrivenException(AUTHOR_MISSING_BIO);
        }
}

    private void checkInput(CreateAuthorRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new EventdrivenException(AUTHOR_MISSING_NAME);
        }
        if (createRequest.getBio() == null) {
            throw new EventdrivenException(AUTHOR_MISSING_BIO);
        }
}
}