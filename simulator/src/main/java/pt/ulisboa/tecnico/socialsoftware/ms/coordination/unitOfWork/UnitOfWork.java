package pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SagaUnitOfWork.class, name = "sagaUnitOfWork"),
        @JsonSubTypes.Type(value = CausalUnitOfWork.class, name = "causalUnitOfWork")
})
public abstract class UnitOfWork implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer version;
    @JsonDeserialize(contentAs = Aggregate.class)
    private Map<Integer, Aggregate> aggregatesToCommit;
    private final Set<Event> eventsToEmit;
    private String functionalityName;

    protected UnitOfWork() {
        this.aggregatesToCommit = new HashMap<>();
        this.eventsToEmit = new HashSet<>();
    }

    public UnitOfWork(Integer version, String functionalityName) {
        this.aggregatesToCommit = new HashMap<>();
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

    public Map<Integer, Aggregate> getAggregatesToCommit() {
        return this.aggregatesToCommit;
    }

    public Set<Event> getEventsToEmit() {
        return eventsToEmit;
    }

    public void addEvent(Event event) {
        this.eventsToEmit.add(event);
    }
}
