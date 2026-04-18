package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.io.Serializable;

public class Command implements Serializable {

    private Integer rootAggregateId;
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


    public UnitOfWork getUnitOfWork() {
        return unitOfWork;
    }

    public String getServiceName() {
        return serviceName;
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