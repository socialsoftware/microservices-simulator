package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.GetTournamentParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

/**
 * Generates a human-readable summary report of a tournament: a header
 * (schedule, number of questions, topic names) followed by one detail block
 * per participant.
 * <p>
 * This is a read-only saga that serves as the engineered NON_REPEATABLE_READ
 * subject. Every step here does the locally correct thing:
 * the tournament is loaded ONCE and its DTO is cached in the workflow
 * ({@link #summaryTournamentDto}) — no step ever re-fetches it on purpose.
 *
 * <h2>What goes wrong (the error)</h2>
 *
 * The tournament is nevertheless read once more PER PARTICIPANT, because
 * {@code fetchParticipantDetailsStep} renders each participant's detail block
 * with {@link GetTournamentParticipantCommand} — the classic per-item
 * enrichment (N+1) pattern. The command is about the PARTICIPANT, but
 * participants live inside the Tournament aggregate, so each call loads the
 * tournament again. The developer never wrote "re-read the tournament";
 * the re-read emerges from composing two individually correct reads (a detail
 * view + a per-item lookup) inside one saga, and sagas provide no isolation
 * between steps. A concurrent functionality that writes the tournament between
 * {@code getTournamentStep} and {@code fetchParticipantDetailsStep} makes the
 * two reads observe different versions of it.
 *
 * <h2>The fix (not applied, on purpose)</h2>
 *
 * Render the detail blocks from the participants already present in the cached
 * DTO instead of issuing per-participant lookups, or provide a command that
 * returns the tournament and its participants' details in one read.
 */
public class GenerateTournamentSummaryFunctionalitySagas extends WorkflowFunctionality {

    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    /** the single, properly cached tournament load (header + participant list) */
    private TournamentDto summaryTournamentDto;

    /** header enrichment: the resolved topic names */
    private final List<String> topicNames = new ArrayList<>();

    /**
     * participant detail blocks:
     * participant user aggregate id -> the details fetched for the report
     * (each fetched by the hidden re-read of the tournament)
     */
    private final Map<Integer, UserDto> participantDetails = new LinkedHashMap<>();

    public GenerateTournamentSummaryFunctionalitySagas(
            SagaUnitOfWorkService unitOfWorkService,
            Integer tournamentAggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {

        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;

        this.buildWorkflow(tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // The ONE deliberate tournament load. The full DTO is cached in the
        // workflow: header fields, participant list, course execution — later
        // steps reuse it instead of re-fetching.
        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand getTournamentCommand = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            this.summaryTournamentDto = (TournamentDto) commandGateway.send(getTournamentCommand);
        });

        // TODO this is using multiple commands, a step should only have one, maybe
        // * change to just fetch first item
        // Header enrichment: resolves each topic id to its current name.
        SagaStep resolveTopicNamesStep = new SagaStep("resolveTopicNamesStep", () -> {
            for (TopicDto topic : this.summaryTournamentDto.getTopics()) {
                GetTopicByIdCommand getTopicCommand = new GetTopicByIdCommand(
                        unitOfWork, ServiceMapping.TOPIC.getServiceName(), topic.getAggregateId());
                TopicDto resolvedTopic = (TopicDto) commandGateway.send(getTopicCommand);
                this.topicNames.add(resolvedTopic.getName());
            }
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        // TODO this is using multiple commands, a step should only have one, maybe
        // * change to just fetch first item
        // Participant detail blocks — THE HIDDEN SECOND READ: the participant
        // lookup command loads the tournament (participants live inside it), so
        // each iteration re-reads the tournament. Locally this step is sound —
        // it iterates the cached participant list — but nothing
        // guarantees the tournament each lookup reads is still the version the
        // cached DTO describes.
        SagaStep fetchParticipantDetailsStep = new SagaStep("fetchParticipantDetailsStep", () -> {
            for (UserDto participant : this.summaryTournamentDto.getParticipants()) {
                GetTournamentParticipantCommand getParticipantCommand = new GetTournamentParticipantCommand(
                        unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                        tournamentAggregateId, participant.getAggregateId());
                UserDto details = (UserDto) commandGateway.send(getParticipantCommand);
                this.participantDetails.put(participant.getAggregateId(), details);
            }
        }, new ArrayList<>(Arrays.asList(getTournamentStep, resolveTopicNamesStep)));

        workflow.addStep(getTournamentStep);
        workflow.addStep(resolveTopicNamesStep);
        workflow.addStep(fetchParticipantDetailsStep);
    }

    public TournamentDto getSummaryTournamentDto() {
        return summaryTournamentDto;
    }

    public List<String> getTopicNames() {
        return topicNames;
    }

    public Map<Integer, UserDto> getParticipantDetails() {
        return participantDetails;
    }
}
