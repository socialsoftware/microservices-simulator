package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;

import java.util.List;

public class SagaCommand extends Command {

    private Command payload;
    private List<SagaAggregate.SagaState> forbiddenStates;
    private SagaAggregate.SagaState semanticLock;

    protected SagaCommand() {}

    public SagaCommand(Command payload) {
        super(payload.getUnitOfWork(), payload.getServiceName(), payload.getRootAggregateId());
        this.payload = payload;
    }

    public Command getPayload() {
        return payload;
    }

    public List<SagaAggregate.SagaState> getForbiddenStates() {
        return forbiddenStates;
    }

    public void setForbiddenStates(List<SagaAggregate.SagaState> forbiddenStates) {
        this.forbiddenStates = forbiddenStates;
    }

    public SagaAggregate.SagaState getSemanticLock() {
        return semanticLock;
    }

    public void setSemanticLock(SagaAggregate.SagaState semanticLock) {
        this.semanticLock = semanticLock;
    }
}
