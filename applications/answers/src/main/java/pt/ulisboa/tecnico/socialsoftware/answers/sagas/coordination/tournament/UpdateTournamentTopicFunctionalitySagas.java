package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentTopicFunctionalitySagas extends WorkflowFunctionality {
    private TournamentTopicDto updatedTopicDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateTournamentTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateTopicStep = new SagaSyncStep("updateTopicStep", () -> {
            TournamentTopicDto updatedTopicDto = tournamentService.updateTournamentTopic(tournamentId, topicAggregateId, topicDto, unitOfWork);
            setUpdatedTopicDto(updatedTopicDto);
        });

        workflow.addStep(updateTopicStep);
    }
    public TournamentTopicDto getUpdatedTopicDto() {
        return updatedTopicDto;
    }

    public void setUpdatedTopicDto(TournamentTopicDto updatedTopicDto) {
        this.updatedTopicDto = updatedTopicDto;
    }
}
