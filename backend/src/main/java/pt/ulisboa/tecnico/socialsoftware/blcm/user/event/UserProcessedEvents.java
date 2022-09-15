package pt.ulisboa.tecnico.socialsoftware.blcm.user.event;

import javax.persistence.*;

@Entity
@Table(name = "user_processed_events")
public class UserProcessedEvents {
    @Id
    @GeneratedValue
    private Integer id;

    @Column
    private Integer lastProcessedEventId;

    public UserProcessedEvents() {

    }

    public UserProcessedEvents(Integer lastProcessedEventId) {
        this.lastProcessedEventId = lastProcessedEventId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLastProcessedEventId() {
        return lastProcessedEventId;
    }

    public void setLastProcessedEventId(Integer lastProcessed) {
        this.lastProcessedEventId = lastProcessed;
    }

}