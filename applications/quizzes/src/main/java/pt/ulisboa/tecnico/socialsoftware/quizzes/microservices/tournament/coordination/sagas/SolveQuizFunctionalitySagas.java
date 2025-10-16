package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.RemoveQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.StartQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.StartTournamentQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.SolveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.states.QuizAnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class SolveQuizFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournament;
    private QuizDto quizDto;
    private QuizAnswerDto quizAnswerDto;
    private Tournament oldTournament;
    private final TournamentService tournamentService;
    private final QuizService quizService;
    private final QuizAnswerService quizAnswerService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public SolveQuizFunctionalitySagas(TournamentService tournamentService, QuizService quizService,
            QuizAnswerService quizAnswerService, SagaUnitOfWorkService unitOfWorkService,
            TournamentFactory tournamentFactory,
            Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
            CommandGateway CommandGateway) {
        this.tournamentService = tournamentService;
        this.quizService = quizService;
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId,
            Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            // TournamentDto tournament = (TournamentDto)
            // tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            // unitOfWorkService.registerSagaState(tournamentAggregateId,
            // TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.READ_TOURNAMENT);
            TournamentDto tournament = (TournamentDto) CommandGateway.send(getTournamentByIdCommand);
            this.setTournament(tournament);
        });

        SagaSyncStep startQuizStep = new SagaSyncStep("startQuizStep", () -> {
            // QuizDto quiz = (QuizDto) quizService.startTournamentQuiz(userAggregateId,
            // this.getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
            // unitOfWorkService.registerSagaState(quiz.getAggregateId(),
            // QuizSagaState.STARTED_TOURNAMENT_QUIZ, unitOfWork);
            StartTournamentQuizCommand startTournamentQuizCommand = new StartTournamentQuizCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), userAggregateId,
                    this.getTournamentDto().getQuiz().getAggregateId());
            startTournamentQuizCommand.setSemanticLock(QuizSagaState.STARTED_TOURNAMENT_QUIZ);
            QuizDto quiz = (QuizDto) CommandGateway.send(startTournamentQuizCommand);
            this.setQuizDto(quiz);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaSyncStep startQuizAnswerStep = new SagaSyncStep("startQuizAnswerStep", () -> {
            // QuizAnswerDto quizAnswerDto = (QuizAnswerDto)
            // quizAnswerService.startQuiz(this.getQuizDto().getAggregateId(),
            // this.getTournamentDto().getCourseExecution().getAggregateId(),
            // userAggregateId, unitOfWork);
            // unitOfWorkService.registerSagaState(quizAnswerDto.getAggregateId(),
            // QuizAnswerSagaState.STARTED_QUIZ, unitOfWork);
            StartQuizCommand startTournamentQuizCommand = new StartQuizCommand(unitOfWork,
                    ServiceMapping.ANSWER.getServiceName(), this.getQuizDto().getAggregateId(),
                    this.getTournamentDto().getCourseExecution().getAggregateId(), userAggregateId);
            startTournamentQuizCommand.setSemanticLock(QuizAnswerSagaState.STARTED_QUIZ);
            QuizAnswerDto quizAnswerDto = (QuizAnswerDto) CommandGateway.send(startTournamentQuizCommand);
            this.setQuizAnswerDto(quizAnswerDto);
        }, new ArrayList<>(Arrays.asList(startQuizStep)));

        startQuizAnswerStep.registerCompensation(() -> {
            if (this.getQuizAnswerDto() != null) {
                // quizAnswerService.removeQuizAnswer(this.getQuizAnswerDto().getAggregateId(),
                // unitOfWork);
                RemoveQuizAnswerCommand removeQuizAnswerCommand = new RemoveQuizAnswerCommand(unitOfWork,
                        ServiceMapping.ANSWER.getServiceName(), this.getQuizAnswerDto().getAggregateId());
                CommandGateway.send(removeQuizAnswerCommand);
            }
        }, unitOfWork);

        SagaSyncStep solveQuizStep = new SagaSyncStep("solveQuizStep", () -> {
            // tournamentService.solveQuiz(tournamentAggregateId, userAggregateId,
            // this.getQuizAnswerDto().getAggregateId(), unitOfWork);
            SolveQuizCommand solveQuizCommand = new SolveQuizCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, userAggregateId,
                    this.getQuizAnswerDto().getAggregateId());
            CommandGateway.send(solveQuizCommand);
        }, new ArrayList<>(Arrays.asList(startQuizAnswerStep)));

        workflow.addStep(getTournamentStep);
        workflow.addStep(startQuizStep);
        workflow.addStep(startQuizAnswerStep);
        workflow.addStep(solveQuizStep);
    }

    public TournamentDto getTournamentDto() {
        return tournament;
    }

    public void setTournament(TournamentDto tournament) {
        this.tournament = tournament;
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