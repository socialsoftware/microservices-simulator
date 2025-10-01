package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTournamentDto;

@Component
public class TournamentSagaFunctionality extends WorkflowFunctionality {
private final TournamentService tournamentService;
private final SagaUnitOfWorkService unitOfWorkService;

public TournamentSagaFunctionality(TournamentService tournamentService, SagaUnitOfWorkService
unitOfWorkService) {
this.tournamentService = tournamentService;
this.unitOfWorkService = unitOfWorkService;
}

    public void addParticipant(Object participant) {
        // TODO: Implement saga functionality for addParticipant
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object findParticipant(Integer userAggregateId) {
        // TODO: Implement saga functionality for findParticipant
        // This method should orchestrate the saga workflow
        return null;
    }

    public Boolean removeParticipant(Object participant) {
        // TODO: Implement saga functionality for removeParticipant
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object findTopic(Integer topicAggregateId) {
        // TODO: Implement saga functionality for findTopic
        // This method should orchestrate the saga workflow
        return null;
    }

    public void removeTopic(Object tournamentTopic) {
        // TODO: Implement saga functionality for removeTopic
        // This method should orchestrate the saga workflow
        return null;
    }

    public void cancel() {
        // TODO: Implement saga functionality for cancel
        // This method should orchestrate the saga workflow
        return null;
    }

    public void remove() {
        // TODO: Implement saga functionality for remove
        // This method should orchestrate the saga workflow
        return null;
    }

    public void setVersion(Integer version) {
        // TODO: Implement saga functionality for setVersion
        // This method should orchestrate the saga workflow
        return null;
    }
}