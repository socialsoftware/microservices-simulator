package pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class UnitOfWork implements Serializable {

    private Integer id;
    private Integer version;
    private final List<Aggregate> aggregatesToCommit;
    private final Set<Event> eventsToEmit;
    private String functionalityName;

    protected UnitOfWork() {
        this.aggregatesToCommit = new ArrayList<>();
        this.eventsToEmit = new HashSet<>();
    }

    public UnitOfWork(Integer version, String functionalityName) {
        this.aggregatesToCommit = new ArrayList<>();
        this.eventsToEmit = new HashSet<>();
        setVersion(version);
        this.functionalityName = functionalityName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getFunctionalityName() {
        return functionalityName;
    }

    public void setFunctionalityName(String functionalityName) {
        this.functionalityName = functionalityName;
    }

    public List<Aggregate> getAggregatesToCommit() {
        return this.aggregatesToCommit;
    }

    public Set<Event> getEventsToEmit() {
        return eventsToEmit;
    }

    public void addEvent(Event event) {
        this.eventsToEmit.add(event);
    }
}
