package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTournamentRequestDto;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto createdTournamentDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateTournamentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, CreateTournamentRequestDto createRequest) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTournamentRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createTournamentStep = new SagaSyncStep("createTournamentStep", () -> {
            TournamentCreator creator = new TournamentCreator(createRequest.getCreator());
            ExecutionDto executionDto = createRequest.getExecution();
            TournamentExecution execution = new TournamentExecution(executionDto);
            QuizDto quizDto = createRequest.getQuiz();
            TournamentQuiz quiz = new TournamentQuiz(quizDto);
            Set<TournamentParticipant> participants = createRequest.getParticipants() != null ? createRequest.getParticipants().stream().map(TournamentParticipant::new).collect(Collectors.toSet()) : null;
            Set<TournamentTopic> topics = null;
            if (createRequest.getTopics() != null) {
                topics = createRequest.getTopics().stream()
                    .map(TournamentTopic::new)
                    .collect(Collectors.toSet());
            }
            TournamentDto createdTournamentDto = tournamentService.createTournament(creator, execution, quiz, createRequest, participants, topics, unitOfWork);
            setCreatedTournamentDto(createdTournamentDto);
        });

        workflow.addStep(createTournamentStep);

    }

    public TournamentDto getCreatedTournamentDto() {
        return createdTournamentDto;
    }

    public void setCreatedTournamentDto(TournamentDto createdTournamentDto) {
        this.createdTournamentDto = createdTournamentDto;
    }
}
