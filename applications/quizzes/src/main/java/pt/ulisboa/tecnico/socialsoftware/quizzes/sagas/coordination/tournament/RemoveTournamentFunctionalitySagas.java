package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.RemoveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.RemoveTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final TournamentService tournamentService;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    private TournamentDto tournamentDto;

    public RemoveTournamentFunctionalitySagas(TournamentService tournamentService, QuizService quizService, SagaUnitOfWorkService unitOfWorkService,
                                Integer tournamentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> { // TODO CAN WE REPLACE VERIFYANDREGISTER??
//            List<SagaAggregate.SagaState> states = new ArrayList<>();
//            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
//            unitOfWorkService.verifyAndRegisterSagaState(tournamentAggregateId, TournamentSagaState.IN_DELETE_TOURNAMENT, states, unitOfWork);
//            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setForbiddenStates(new ArrayList<>(List.of(TournamentSagaState.IN_UPDATE_TOURNAMENT)));
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.IN_DELETE_TOURNAMENT);
            TournamentDto tournamentDto = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
            setTournamentDto(tournamentDto);
        });

        SagaSyncStep removeQuizStep = new SagaSyncStep("removeQuizStep", () -> {
//            quizService.removeQuiz(getTournamentDto().getQuiz().getAggregateId(), unitOfWork);
            RemoveQuizCommand removeQuizCommand = new RemoveQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), getTournamentDto().getQuiz().getAggregateId());
            commandGateway.send(removeQuizCommand);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        SagaSyncStep removeTournamentStep = new SagaSyncStep("removeTournamentStep", () -> {
//            tournamentService.removeTournament(tournamentAggregateId, unitOfWork);
            RemoveTournamentCommand removeTournamentCommand = new RemoveTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            commandGateway.send(removeTournamentCommand);
        }, new ArrayList<>(Arrays.asList(removeQuizStep)));

        workflow.addStep(getTournamentStep);
        workflow.addStep(removeQuizStep);
        workflow.addStep(removeTournamentStep);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }
}