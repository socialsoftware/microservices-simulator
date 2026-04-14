package pt.ulisboa.tecnico.socialsoftware.showcase.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UserLoyaltyAwardedEvent extends Event {
    private Integer userAggregateId;
    private Integer pointsAwarded;

    public UserLoyaltyAwardedEvent() {
        super();
    }

    public UserLoyaltyAwardedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public UserLoyaltyAwardedEvent(Integer aggregateId, Integer userAggregateId, Integer pointsAwarded) {
        super(aggregateId);
        setUserAggregateId(userAggregateId);
        setPointsAwarded(pointsAwarded);
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(Integer pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

}