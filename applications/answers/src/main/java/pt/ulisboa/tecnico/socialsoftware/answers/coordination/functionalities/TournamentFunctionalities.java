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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import java.util.List;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private TopicService topicService;

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

    public TournamentDto createTournament(TournamentDto tournamentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(tournamentDto);
                CreateTournamentFunctionalitySagas createTournamentFunctionalitySagas = new CreateTournamentFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService, executionService, quizService, topicService, tournamentDto);
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

    public List<TournamentDto> searchTournaments(Boolean cancelled, Integer creatorAggregateId, Integer executionAggregateId, Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchTournamentsFunctionalitySagas searchTournamentsFunctionalitySagas = new SearchTournamentsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, tournamentService, cancelled, creatorAggregateId, executionAggregateId, quizAggregateId);
                searchTournamentsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchTournamentsFunctionalitySagas.getSearchedTournamentDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TournamentDto tournamentDto) {
}
}