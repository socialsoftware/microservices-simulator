package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.CountUserTournamentsInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

/**
 * Adds a user as a participant of a tournament, but only if the user is not
 * already a participant of {@link #MAX_TOURNAMENTS_PER_USER} tournaments of the
 * same course execution.
 * <p>
 * This is the quota-guarded twin of {@link AddParticipantFunctionalitySagas}
 * and serves as the engineered write-skew subject. The quota is the only
 * behavioural difference between them.
 *
 * <h2>The anomaly</h2>
 *
 * The quota is read in {@code countUserTournamentsStep} and applied later in
 * {@code addParticipantStep}. Two concurrent instances adding the same user to
 * two different tournaments can both read the quota as satisfied before either
 * has added, so both add: the user ends up over the quota, breaking the
 * max-tournaments inter-invariant — a state no serial execution produces.
 *
 * <h2>Why the existing mechanisms do not catch it</h2>
 *
 * Every consistency mechanism here is scoped to a SINGLE aggregate:
 * {@link TournamentSagaState#IN_UPDATE_TOURNAMENT} semantic lock guards one
 * tournament, {@code verifyInvariants()} validates one aggregate, and
 * versioning / re-read / merge resolves conflicts on one aggregate. The two
 * adds write two DIFFERENT Tournament aggregates, so they never meet: there is
 * no shared aggregate for any of these to conflict on.
 * 
 * <h2>A real fix: reserve instead of check</h2>
 *
 * Keep the aggregate boundaries, but turn the check into a WRITE: reserve a
 * quota slot on the single aggregate that owns the quota (the user's record in
 * the course execution) before adding the participant, and compensate by
 * releasing the slot on abort. Both sagas then write that same aggregate, where
 * versioning and {@code verifyInvariants()} do apply, so the second one is
 * forced to see the first. Writes conflict; reads do not.
 */
public class AddParticipantWithinMaxTournamentsFunctionalitySagas extends WorkflowFunctionality {

    /**
     * The quota this functionality enforces: a user may participate in at most this
     * many tournaments of the same course execution. This is the authoritative
     * value — the domain rule lives here, with the only code that enforces it.
     */
    public static final int MAX_TOURNAMENTS_PER_USER = 1;

    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;
    private UserDto userDto;
    private int userTournamentCount;

    public AddParticipantWithinMaxTournamentsFunctionalitySagas(
            SagaUnitOfWorkService unitOfWorkService,
            Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {

        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;

        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(
            Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork) {

        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetStudentByExecutionIdAndUserIdCommand getStudentCommand = new GetStudentByExecutionIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userAggregateId);
            this.userDto = (UserDto) commandGateway.send(getStudentCommand);
        });

        // Reads the quota across every tournament of the execution.
        // This is only a READ, so it registers no conflict anywhere: nothing stops a
        // concurrent saga from reading the same count and reaching the same conclusion.
        SagaStep countUserTournamentsStep = new SagaStep("countUserTournamentsStep", () -> {
            CountUserTournamentsInExecutionCommand countCommand = new CountUserTournamentsInExecutionCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId, userAggregateId);
            this.userTournamentCount = (Integer) commandGateway.send(countCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        SagaStep addParticipantStep = new SagaStep("addParticipantStep", () -> {
            // Acts on the count observed earlier, and deliberately does not re-read it:
            // re-reading here would not help anyway (see the class javadoc), it would
            // only make the anomaly rarer and harder to reproduce.
            if (this.userTournamentCount < MAX_TOURNAMENTS_PER_USER) {
                AddParticipantCommand addParticipantCommand = new AddParticipantCommand(
                        unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                        tournamentAggregateId, this.userDto);

                // Guards this ONE tournament against a concurrent update of itself. It does
                // nothing for the quota: the competing saga adds to a different tournament,
                // so it takes a different lock and the two never collide.
                SagaCommand sagaCommand = new SagaCommand(addParticipantCommand);
                sagaCommand.setForbiddenStates(List.of(TournamentSagaState.IN_UPDATE_TOURNAMENT));
                commandGateway.send(sagaCommand);
            }
        }, new ArrayList<>(Arrays.asList(getUserStep, countUserTournamentsStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(countUserTournamentsStep);
        workflow.addStep(addParticipantStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }
}
