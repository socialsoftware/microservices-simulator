package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

public abstract class EventSubscription {
    private Integer subscribedAggregateId;
    private Integer subscribedVersion;
    private String eventType;
    private Integer subscriberAggregateId;

    public EventSubscription() {
    }

    public EventSubscription(Integer subscribedAggregateId, Integer subscribedVersion, String eventType) {
        setSubscribedAggregateId(subscribedAggregateId);
        // this is for complex functionalities where we don't know the id of an aggregate we are creating
        if (subscribedVersion == null) {
            setSubscribedVersion(0);
        } else {
            setSubscribedVersion(subscribedVersion);
        }
        setEventType(eventType);
        setSubscriberAggregateId(subscribedAggregateId);
    }

    public EventSubscription(EventSubscription other) {
        setSubscribedAggregateId(other.getSubscribedAggregateId());
        setSubscribedVersion(other.getSubscribedVersion());
        setEventType(other.getEventType());
    }


    public boolean subscribesEvent(Event event) {
        return getEventType().equals(event.getClass().getSimpleName()) &&
                getSubscribedAggregateId().equals(event.getPublisherAggregateId()) &&
                getSubscribedVersion() < event.getPublisherAggregateVersion();
    }


    public Integer getSubscribedAggregateId() {
        return subscribedAggregateId;
    }

    public void setSubscribedAggregateId(Integer subscribedAggregateId) {
        this.subscribedAggregateId = subscribedAggregateId;
    }

    public Integer getSubscribedVersion() {
        return subscribedVersion;
    }

    public void setSubscribedVersion(Integer subscribedVersion) {
        this.subscribedVersion = subscribedVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getSubscriberAggregateId() {
        return subscriberAggregateId;
    }

    public void setSubscriberAggregateId(Integer subscriberAggregateId) {
        this.subscriberAggregateId = subscriberAggregateId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EventSubscription)) {
            return false;
        }
        EventSubscription other = (EventSubscription) obj;
        return getSubscribedAggregateId() != null && getSubscribedAggregateId().equals(other.getSubscribedAggregateId()) &&
                getSubscribedVersion() != null && getSubscribedVersion().equals(other.getSubscribedVersion()) &&
                getEventType() != null && getEventType().equals(other.getEventType());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getSubscribedAggregateId();
        hash = 31 * hash + (getSubscribedVersion() == null ? 0 : getSubscribedVersion().hashCode());
        hash = 31 * hash + getEventType().hashCode();
        return hash;
    }
}