package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTournamentRequestDto;
import java.util.List;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService, createRequest);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService, tournamentAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService, tournamentDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService, tournamentAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, participantAggregateId, participantDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, participantDtos);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, participantAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, participantAggregateId, participantDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, participantAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, topicAggregateId, topicDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, topicDtos);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, topicAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, topicAggregateId, topicDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService,
                        tournamentId, topicAggregateId);
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