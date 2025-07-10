package pt.ulisboa.tecnico.socialsoftware.quizzes.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

import java.util.List;

public class VerifySagaStateCommand implements Command {
    private final Integer aggregateId;
    private final List<SagaAggregate.SagaState> forbiddenStates;

    public VerifySagaStateCommand(Integer aggregateId, List<SagaAggregate.SagaState> forbiddenStates) {
        this.aggregateId = aggregateId;
        this.forbiddenStates = forbiddenStates;
    }
}
