package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
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
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class SolveQuizFunctionality extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private QuizDto quizDto;
    private QuizAnswerDto quizAnswerDto;
    private Tournament oldTournament;

    private SagaWorkflow workflow;

    private final TournamentService tournamentService;
    private final QuizService quizService;
    private final QuizAnswerService quizAnswerService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public SolveQuizFunctionality(TournamentService tournamentService, QuizService quizService, QuizAnswerService quizAnswerService, SagaUnitOfWorkService unitOfWorkService, 
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

        SyncStep getTournamentStep = new SyncStep("getTournamentStep", () -> {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, SagaState.SOLVE_QUIZ_READ_TOURNAMENT, unitOfWork);
            this.setOldTournament(tournament);
            this.setTournamentDto(tournamentDto);
        });
    
        getTournamentStep.registerCompensation(() -> {
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep startQuizStep = new SyncStep("startQuizStep", () -> {
            QuizDto quizDto = quizService.startTournamentQuiz(userAggregateId, this.getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.SOLVE_QUIZ_STARTED_TOURNAMENT_QUIZ, unitOfWork);
            this.setQuizDto(quizDto);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        startQuizStep.registerCompensation(() -> {
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(this.getQuizDto().getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep startQuizAnswerStep = new SyncStep("startQuizAnswerStep", () -> {
            QuizAnswerDto quizAnswerDto = quizAnswerService.startQuiz(this.getQuizDto().getAggregateId(), this.getTournamentDto().getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            SagaQuizAnswer quizAnswer = (SagaQuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswer, SagaState.SOLVE_QUIZ_STARTED_QUIZ, unitOfWork);
            this.setQuizAnswerDto(quizAnswerDto);
        }, new ArrayList<>(Arrays.asList(startQuizStep)));
    
        startQuizAnswerStep.registerCompensation(() -> {
            QuizAnswerDto quizAnswerDto = this.getQuizAnswerDto();
            quizAnswerDto.setState(SagaState.NOT_IN_SAGA.toString());
        }, unitOfWork);
        
        SyncStep solveQuizStep = new SyncStep("solveQuizStep", () -> {
            tournamentService.solveQuiz(tournamentAggregateId, userAggregateId, this.getQuizAnswerDto().getAggregateId(), unitOfWork);
        });
        
        solveQuizStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(this.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);

        workflow.addStep(getTournamentStep);
        workflow.addStep(startQuizStep);
        workflow.addStep(startQuizAnswerStep);
        workflow.addStep(solveQuizStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }

    public void setQuizDto(QuizDto quizDto) {
        this.quizDto = quizDto;
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }

    public void setQuizAnswerDto(QuizAnswerDto quizAnswerDto) {
        this.quizAnswerDto = quizAnswerDto;
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }
}