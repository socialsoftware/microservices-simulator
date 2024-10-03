package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuizAnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class SolveQuizFunctionalitySagas extends WorkflowFunctionality {
    private SagaTournamentDto tournament;
    private SagaQuizDto quizDto;
    private SagaQuizAnswerDto quizAnswerDto;
    private Tournament oldTournament;

    

    private final TournamentService tournamentService;
    private final QuizService quizService;
    private final QuizAnswerService quizAnswerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public SolveQuizFunctionalitySagas(TournamentService tournamentService, QuizService quizService, QuizAnswerService quizAnswerService, SagaUnitOfWorkService unitOfWorkService, 
                                TournamentFactory tournamentFactory,
                                Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.quizService = quizService;
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            SagaTournamentDto tournament = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            this.setTournament(tournament);
        });
    
        getTournamentStep.registerCompensation(() -> {
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep startQuizStep = new SagaSyncStep("startQuizStep", () -> {
            SagaQuizDto quiz = (SagaQuizDto) quizService.startTournamentQuiz(userAggregateId, this.getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz.getAggregateId(), QuizSagaState.STARTED_TOURNAMENT_QUIZ, unitOfWork);
            this.setQuizDto(quiz);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        startQuizStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(this.getQuizDto().getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep startQuizAnswerStep = new SagaSyncStep("startQuizAnswerStep", () -> {
            SagaQuizAnswerDto quizAnswerDto = (SagaQuizAnswerDto) quizAnswerService.startQuiz(this.getQuizDto().getAggregateId(), this.getTournamentDto().getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswerDto.getAggregateId(), QuizAnswerSagaState.STARTED_QUIZ, unitOfWork);
            this.setQuizAnswerDto(quizAnswerDto);
        }, new ArrayList<>(Arrays.asList(startQuizStep)));
    
        startQuizAnswerStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(this.getQuizAnswerDto().getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
        
        SagaSyncStep solveQuizStep = new SagaSyncStep("solveQuizStep", () -> {
            tournamentService.solveQuiz(tournamentAggregateId, userAggregateId, this.getQuizAnswerDto().getAggregateId(), unitOfWork);
        });

        workflow.addStep(getTournamentStep);
        workflow.addStep(startQuizStep);
        workflow.addStep(startQuizAnswerStep);
        workflow.addStep(solveQuizStep);
    }

    @Override
    public void handleEvents() {

    }
    

    public SagaTournamentDto getTournamentDto() {
        return tournament;
    }

    public void setTournament(SagaTournamentDto tournament) {
        this.tournament = tournament;
    }

    public SagaQuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(SagaQuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public SagaQuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }

    public void setQuizAnswerDto(SagaQuizAnswerDto quizAnswerDto) {
        this.quizAnswerDto = quizAnswerDto;
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }
}