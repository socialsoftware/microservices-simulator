package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


public class Command implements Serializable {

    private Integer rootAggregateId;
    private List<SagaAggregate.SagaState> forbiddenStates; // sagas
    private SagaAggregate.SagaState semanticLock; // sagas
    private UnitOfWork unitOfWork;
    private String serviceName;

    protected Command() {}

    public Command(UnitOfWork unitOfWork, String serviceName, Integer rootAggregateId) {
        this.unitOfWork = unitOfWork;
        this.serviceName = serviceName;
        this.rootAggregateId = rootAggregateId;
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

    // Getters and setters for JSON deserialization
    public void setRootAggregateId(Integer rootAggregateId) {
        this.rootAggregateId = rootAggregateId;
    }

    public void setUnitOfWork(UnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}