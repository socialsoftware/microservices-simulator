package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.UpdateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.states.QuizSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private List<TopicDto> topicDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer tournamentId,
                                               LocalDateTime startTime, LocalDateTime endTime,
                                               List<Integer> topicIds,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, startTime, endTime, topicIds, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId,
                               LocalDateTime startTime, LocalDateTime endTime,
                               List<Integer> topicIds,
                               SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        this.topicDtos = new ArrayList<>();

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand getCmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            this.tournamentDto = (TournamentDto) commandGateway.send(sagaCmd);
        });

        SagaStep getTopicsStep = new SagaStep("getTopicsStep", () -> {
            for (Integer topicId : topicIds) {
                GetTopicByIdCommand cmd = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicId);
                TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
                this.topicDtos.add(topicDto);
            }
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaStep updateTournamentStep = new SagaStep("updateTournamentStep", () -> {
            UpdateTournamentCommand cmd = new UpdateTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentId, startTime, endTime,
                    this.topicDtos.isEmpty() ? null : this.topicDtos);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTopicsStep)));

        SagaStep updateQuizStep = new SagaStep("updateQuizStep", () -> {
            UpdateQuizCommand cmd = new UpdateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(),
                    this.tournamentDto.getQuizAggregateId(),
                    startTime, endTime, endTime, null);
            SagaCommand sagaCmd = new SagaCommand(cmd);
            sagaCmd.setForbiddenStates(new ArrayList<>(Arrays.asList(
                    QuizSagaState.IN_UPDATE_QUIZ,
                    QuizSagaState.READ_QUIZ)));
            commandGateway.send(sagaCmd);
        }, new ArrayList<>(Arrays.asList(updateTournamentStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getTopicsStep);
        this.workflow.addStep(updateTournamentStep);
        this.workflow.addStep(updateQuizStep);
    }
}
