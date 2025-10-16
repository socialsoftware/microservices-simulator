package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
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

@SuppressWarnings("unused")
public class SolveQuizFunctionalityTCC extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private QuizDto quizDto;
    private QuizAnswerDto quizAnswerDto;
    private Tournament oldTournament;
    private final TournamentService tournamentService;
    private final QuizService quizService;
    private final QuizAnswerService quizAnswerService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public SolveQuizFunctionalityTCC(TournamentService tournamentService, QuizService quizService,
            QuizAnswerService quizAnswerService, CausalUnitOfWorkService unitOfWorkService,
            TournamentFactory tournamentFactory,
            Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.quizService = quizService;
        this.quizAnswerService = quizAnswerService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId,
            Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // TournamentDto tournamentDto =
            // tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            TournamentDto tournamentDto = (TournamentDto) commandGateway.send(new GetTournamentByIdCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId));

            // this.quizDto = quizService.startTournamentQuiz(userAggregateId,
            // tournamentDto.getQuiz().getAggregateId(), unitOfWork);
            StartTournamentQuizCommand StartTournamentQuizCommand = new StartTournamentQuizCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), userAggregateId, tournamentDto.getQuiz().getAggregateId());
            this.quizDto = (QuizDto) commandGateway.send(StartTournamentQuizCommand);

            // QuizAnswerDto quizAnswerDto =
            // quizAnswerService.startQuiz(tournamentDto.getQuiz().getAggregateId(),
            // tournamentDto.getCourseExecution().getAggregateId(), userAggregateId,
            // unitOfWork);
            StartQuizCommand startQuizCommand = new StartQuizCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(),
                    quizDto.getAggregateId(), userAggregateId, tournamentDto.getCourseExecution().getAggregateId());
            quizAnswerDto = (QuizAnswerDto) commandGateway.send(startQuizCommand);

            // tournamentService.solveQuiz(tournamentAggregateId, userAggregateId,
            // quizAnswerDto.getAggregateId(), unitOfWork);
            SolveQuizCommand SolveQuizCommand = new SolveQuizCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, userAggregateId,
                    quizAnswerDto.getAggregateId());
            commandGateway.send(SolveQuizCommand);
        });

        workflow.addStep(step);
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