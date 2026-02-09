package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddTournamentTopicsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentTopicDto> addedTopicDtos;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddTournamentTopicsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, List<TournamentTopicDto> topicDtos) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, topicDtos, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, List<TournamentTopicDto> topicDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addTopicsStep = new SagaSyncStep("addTopicsStep", () -> {
            List<TournamentTopicDto> addedTopicDtos = tournamentService.addTournamentTopics(tournamentId, topicDtos, unitOfWork);
            setAddedTopicDtos(addedTopicDtos);
        });

        workflow.addStep(addTopicsStep);
    }
    public List<TournamentTopicDto> getAddedTopicDtos() {
        return addedTopicDtos;
    }

    public void setAddedTopicDtos(List<TournamentTopicDto> addedTopicDtos) {
        this.addedTopicDtos = addedTopicDtos;
    }
}
