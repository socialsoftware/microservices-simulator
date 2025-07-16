package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.h2.tools.Server;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

import java.util.List;

public abstract class Command {
    private Integer rootAggregateId;
    private List<SagaAggregate.SagaState> forbiddenStates;
    private SagaAggregate.SagaState semanticLock;
    private UnitOfWork unitOfWork;
    private String serviceName;

    public Command(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId) {
        this.unitOfWork = unitOfWork;
        this.serviceName = serviceName;
        this.rootAggregateId = rootAggregateId;
    }

    public Command(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId, List<SagaAggregate.SagaState> forbiddenStates, SagaAggregate.SagaState semanticLock) {
        this(unitOfWork, serviceName, rootAggregateId);
        this.rootAggregateId = rootAggregateId;
        this.forbiddenStates = forbiddenStates;
        this.semanticLock = semanticLock;
    }

    public Integer getRootAggregateId() {
        return rootAggregateId;
    }

    public List<SagaAggregate.SagaState> getForbiddenStates() {
        return forbiddenStates;
    }

    public SagaAggregate.SagaState getSemanticLock() {
        return semanticLock;
    }

    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setForbiddenStates(List<SagaAggregate.SagaState> forbiddenStates) {
        this.forbiddenStates = forbiddenStates;
    }

    public void setSemanticLock(SagaAggregate.SagaState semanticLock) {
        this.semanticLock = semanticLock;
    }
}