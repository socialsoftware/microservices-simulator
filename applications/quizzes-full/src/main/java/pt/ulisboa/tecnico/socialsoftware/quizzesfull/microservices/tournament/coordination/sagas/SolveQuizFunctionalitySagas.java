package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.GetQuizAnswerByQuizIdAndStudentIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.SolveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;

import java.util.ArrayList;
import java.util.Arrays;

public class SolveQuizFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private QuizAnswerDto quizAnswerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public SolveQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        Integer tournamentId, Integer userId,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, userId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer userId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand cmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId);
            this.tournamentDto = (TournamentDto) commandGateway.send(cmd);
        });

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerByQuizIdAndStudentIdCommand cmd = new GetQuizAnswerByQuizIdAndStudentIdCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(),
                    this.tournamentDto.getQuizAggregateId(), userId);
            this.quizAnswerDto = (QuizAnswerDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaStep solveQuizStep = new SagaStep("solveQuizStep", () -> {
            SolveQuizCommand cmd = new SolveQuizCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentId, userId,
                    this.quizAnswerDto.getAggregateId(), this.quizAnswerDto.getVersion());
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getQuizAnswerStep);
        this.workflow.addStep(solveQuizStep);
    }
}
