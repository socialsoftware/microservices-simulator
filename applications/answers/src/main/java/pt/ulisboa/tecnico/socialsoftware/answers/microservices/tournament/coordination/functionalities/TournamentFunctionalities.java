package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto;
import java.util.List;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService;

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
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto createTournament(CreateTournamentRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateTournamentFunctionalitySagas createTournamentFunctionalitySagas = new CreateTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTournamentFunctionalitySagas.getCreatedTournamentDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto getTournamentById(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentByIdFunctionalitySagas getTournamentByIdFunctionalitySagas = new GetTournamentByIdFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId, sagaUnitOfWork, commandGateway);
                getTournamentByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTournamentByIdFunctionalitySagas.getTournamentDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto updateTournament(TournamentDto tournamentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(tournamentDto);
                UpdateTournamentFunctionalitySagas updateTournamentFunctionalitySagas = new UpdateTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentDto, sagaUnitOfWork, commandGateway);
                updateTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTournamentFunctionalitySagas.getUpdatedTournamentDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTournamentFunctionalitySagas deleteTournamentFunctionalitySagas = new DeleteTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId, sagaUnitOfWork, commandGateway);
                deleteTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getAllTournaments() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllTournamentsFunctionalitySagas getAllTournamentsFunctionalitySagas = new GetAllTournamentsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllTournamentsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllTournamentsFunctionalitySagas.getTournaments();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentParticipantDto addTournamentParticipant(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddTournamentParticipantFunctionalitySagas addTournamentParticipantFunctionalitySagas = new AddTournamentParticipantFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, participantAggregateId, participantDto,
                        sagaUnitOfWork, commandGateway);
                addTournamentParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addTournamentParticipantFunctionalitySagas.getAddedParticipantDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentParticipantDto> addTournamentParticipants(Integer tournamentId, List<TournamentParticipantDto> participantDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddTournamentParticipantsFunctionalitySagas addTournamentParticipantsFunctionalitySagas = new AddTournamentParticipantsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, participantDtos,
                        sagaUnitOfWork, commandGateway);
                addTournamentParticipantsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addTournamentParticipantsFunctionalitySagas.getAddedParticipantDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentParticipantDto getTournamentParticipant(Integer tournamentId, Integer participantAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentParticipantFunctionalitySagas getTournamentParticipantFunctionalitySagas = new GetTournamentParticipantFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, participantAggregateId,
                        sagaUnitOfWork, commandGateway);
                getTournamentParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTournamentParticipantFunctionalitySagas.getParticipantDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentParticipantDto updateTournamentParticipant(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentParticipantFunctionalitySagas updateTournamentParticipantFunctionalitySagas = new UpdateTournamentParticipantFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, participantAggregateId, participantDto,
                        sagaUnitOfWork, commandGateway);
                updateTournamentParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTournamentParticipantFunctionalitySagas.getUpdatedParticipantDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeTournamentParticipant(Integer tournamentId, Integer participantAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveTournamentParticipantFunctionalitySagas removeTournamentParticipantFunctionalitySagas = new RemoveTournamentParticipantFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, participantAggregateId,
                        sagaUnitOfWork, commandGateway);
                removeTournamentParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentTopicDto addTournamentTopic(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddTournamentTopicFunctionalitySagas addTournamentTopicFunctionalitySagas = new AddTournamentTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, topicAggregateId, topicDto,
                        sagaUnitOfWork, commandGateway);
                addTournamentTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addTournamentTopicFunctionalitySagas.getAddedTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentTopicDto> addTournamentTopics(Integer tournamentId, List<TournamentTopicDto> topicDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddTournamentTopicsFunctionalitySagas addTournamentTopicsFunctionalitySagas = new AddTournamentTopicsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, topicDtos,
                        sagaUnitOfWork, commandGateway);
                addTournamentTopicsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addTournamentTopicsFunctionalitySagas.getAddedTopicDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentTopicDto getTournamentTopic(Integer tournamentId, Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentTopicFunctionalitySagas getTournamentTopicFunctionalitySagas = new GetTournamentTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, topicAggregateId,
                        sagaUnitOfWork, commandGateway);
                getTournamentTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTournamentTopicFunctionalitySagas.getTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentTopicDto updateTournamentTopic(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentTopicFunctionalitySagas updateTournamentTopicFunctionalitySagas = new UpdateTournamentTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, topicAggregateId, topicDto,
                        sagaUnitOfWork, commandGateway);
                updateTournamentTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTournamentTopicFunctionalitySagas.getUpdatedTopicDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeTournamentTopic(Integer tournamentId, Integer topicAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveTournamentTopicFunctionalitySagas removeTournamentTopicFunctionalitySagas = new RemoveTournamentTopicFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentId, topicAggregateId,
                        sagaUnitOfWork, commandGateway);
                removeTournamentTopicFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TournamentDto tournamentDto) {
}

    private void checkInput(CreateTournamentRequestDto createRequest) {
}
}