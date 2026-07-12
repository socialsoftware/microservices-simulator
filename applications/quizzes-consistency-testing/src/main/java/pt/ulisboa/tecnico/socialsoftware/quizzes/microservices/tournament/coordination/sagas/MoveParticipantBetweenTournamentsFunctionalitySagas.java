package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.LeaveTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

/**
 * Moves a participant from one tournament to another: it removes the user from
 * the source tournament and then enrols them in the target one.
 * If the enrolment in the target fails, the compensation puts the user back
 * into the source tournament.
 *
 * <h2>The anomaly it exposes (a compensation that becomes impossible)</h2>
 *
 * The compensation of {@code leaveSourceTournamentStep} is perfectly correct in
 * isolation: whatever the target tournament does, the user ends up back where
 * they started. But in sagas there is no isolation - {@code registerChanged}
 * publishes the new source-tournament version (without the user) as soon as
 * {@code leaveSourceTournamentStep} finishes, long before this saga knows
 * whether it will succeed. A concurrent saga can act on that intermediate
 * version.
 * <p>
 * A dangerous concurrent saga is {@code RemoveTournamentFunctionalitySagas}:
 * a tournament may only be deleted while it has no participants (DELETE
 * invariant, {@code state == DELETED => participants.empty}).
 * If it observes the source tournament in the window where this saga has
 * already taken the user out, the delete looks legal and goes through. This
 * saga then fails on the target and tries to compensate - and the re-add is now
 * rejected by that same DELETE invariant, because the tournament it wants to
 * put the user back into no longer exists.
 * <p>
 * The result is a saga that neither committed nor rolled back: the user is in
 * NEITHER tournament, and the source tournament is gone. No serial execution
 * produces this - run alone, the delete would have refused (the participant is
 * still there) and the compensation would have succeeded.
 *
 * <h2>Why the existing mechanisms do not catch it</h2>
 *
 * Every individual write is valid at the moment it is made, so
 * {@code verifyInvariants()} never breaks: the leave is valid, the delete is
 * valid against the version it saw, and only the compensation - the write that
 * is supposed to be always safe - is rejected. Versioning does not help either:
 * the deleting saga really did read the latest version of the source
 * tournament. The mechanism that could have covered this is a semantic lock:
 * this saga should lock the source tournament while the move is in flight, and
 * {@code RemoveTournamentFunctionalitySagas} should list that state among its
 * forbidden ones.
 */
public class MoveParticipantBetweenTournamentsFunctionalitySagas extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private UserDto userDto;

    public MoveParticipantBetweenTournamentsFunctionalitySagas(
            SagaUnitOfWorkService unitOfWorkService,
            Integer sourceTournamentAggregateId, Integer targetTournamentAggregateId,
            Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {

        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;

        this.buildWorkflow(sourceTournamentAggregateId, targetTournamentAggregateId,
                executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(
            Integer sourceTournamentAggregateId, Integer targetTournamentAggregateId,
            Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork) {

        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetStudentByExecutionIdAndUserIdCommand getStudentCommand = new GetStudentByExecutionIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(),
                    executionAggregateId, userAggregateId);
            this.userDto = (UserDto) commandGateway.send(getStudentCommand);
        });

        // No semantic lock is taken on the source tournament, so nothing stops another
        // saga from acting on the version this step publishes - a version in which the
        // user is already gone, even though the move may still be undone.
        SagaStep leaveSourceTournamentStep = new SagaStep("leaveSourceTournamentStep", () -> {
            LeaveTournamentCommand leaveTournamentCommand = new LeaveTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    sourceTournamentAggregateId, userAggregateId);
            commandGateway.send(leaveTournamentCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        // Puts the user back into the source tournament if the move fails
        // (compensation).
        //
        // NOTE: this is a plain addParticipant, so the restored participant gets a NEW
        // enrollTime (TournamentParticipant stamps DateHandler.now()) instead of the
        // one they originally had. The compensation is therefore not an exact inverse:
        // it silently rewrites the enroll time and, if the source tournament has
        // started in the meantime, ENROLL_UNTIL_START_TIME will refuse to let back in a
        // user who was legitimately enrolled. A faithful compensation would need to
        // restore the original enrollTime. That flaw only shows up after real
        // wall-clock time has passed, not as a consequence of any interleaving, so it
        // is out of scope for concurrency testing and is deliberately left as is.
        leaveSourceTournamentStep.registerCompensation(() -> {
            AddParticipantCommand addParticipantCommand = new AddParticipantCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    sourceTournamentAggregateId, this.userDto);
            commandGateway.send(addParticipantCommand);
        }, unitOfWork);

        SagaStep addToTargetTournamentStep = new SagaStep("addToTargetTournamentStep", () -> {
            AddParticipantCommand addParticipantCommand = new AddParticipantCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    targetTournamentAggregateId, this.userDto);
            commandGateway.send(addParticipantCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep, leaveSourceTournamentStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(leaveSourceTournamentStep);
        workflow.addStep(addToTargetTournamentStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
