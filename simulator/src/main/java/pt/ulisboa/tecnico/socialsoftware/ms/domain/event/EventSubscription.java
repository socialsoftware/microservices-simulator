package pt.ulisboa.tecnico.socialsoftware.ms.domain.event;

import java.util.Objects;

public abstract class EventSubscription {
    private Integer subscribedAggregateId;
    private Integer subscribedVersion;
    private Class<? extends Event> eventClass;
    private Integer subscriberAggregateId;

    public EventSubscription() {
    }

    public EventSubscription(Integer subscribedAggregateId, Integer subscribedVersion,
            Class<? extends Event> eventClass) {
        setSubscribedAggregateId(subscribedAggregateId);
        // this is for complex functionalities where we don't know the id of an
        // aggregate we are creating
        setSubscribedVersion(Objects.requireNonNullElse(subscribedVersion, 0));
        setEventType(eventType);
        setSubscriberAggregateId(subscribedAggregateId);
    }

    public EventSubscription(EventSubscription other) {
        setSubscribedAggregateId(other.getSubscribedAggregateId());
        setSubscribedVersion(other.getSubscribedVersion());
        setEventClass(other.getEventClass());
    }

    public boolean subscribesEvent(Event event) {
        return getEventClass().isInstance(event) &&
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

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public void setEventClass(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
    }

    /**
     * Returns the simple name of the event class.
     * Kept for backward compatibility.
     *
     * @return the simple class name (e.g., "UserDeletedEvent")
     */
    public String getEventType() {
        return eventClass != null ? eventClass.getSimpleName() : null;
    }

    /**
     * Sets the event class from a string class name.
     * Kept for backward compatibility.
     * 
     * @deprecated Use setEventClass(Class) instead
     */
    @Deprecated
    public void setEventType(String eventType) {
        // For backward compatibility, attempt to resolve class from name
        // This is a best-effort approach and may not work in all classloaders
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> clazz = (Class<? extends Event>) Class.forName(
                    "pt.ulisboa.tecnico.socialsoftware.ms.domain.event." + eventType);
            this.eventClass = clazz;
        } catch (ClassNotFoundException e) {
            // Fallback: store null and rely on subclass initialization
            this.eventClass = null;
        }
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
        return getSubscribedAggregateId() != null && getSubscribedAggregateId().equals(other.getSubscribedAggregateId())
                &&
                getSubscribedVersion() != null && getSubscribedVersion().equals(other.getSubscribedVersion()) &&
                getEventClass() != null && getEventClass().equals(other.getEventClass());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getSubscribedAggregateId();
        hash = 31 * hash + (getSubscribedVersion() == null ? 0 : getSubscribedVersion().hashCode());
        hash = 31 * hash + (getEventClass() == null ? 0 : getEventClass().hashCode());
        return hash;
    }
}