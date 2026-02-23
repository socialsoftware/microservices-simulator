package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate.OrderUser;

public class OrderUserDto implements Serializable {
    private String userName;
    private String realName;
    private String email;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public OrderUserDto() {
    }

    public OrderUserDto(OrderUser orderUser) {
        this.userName = orderUser.getUserName();
        this.realName = orderUser.getUserRealName();
        this.email = orderUser.getUserEmail();
        this.aggregateId = orderUser.getUserAggregateId();
        this.version = orderUser.getUserVersion();
        this.state = orderUser.getUserState();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}