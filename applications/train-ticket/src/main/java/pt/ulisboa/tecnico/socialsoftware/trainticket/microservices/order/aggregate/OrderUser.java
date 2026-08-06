package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;

@Entity
public class OrderUser {
    @Id
    @GeneratedValue
    private Long id;
    private Integer userAggregateId;
    private Long userVersion;
    private AggregateState userState;
    @OneToOne
    private Order order;

    public OrderUser() {

    }

    public OrderUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public OrderUser(OrderUserDto orderUserDto) {
        setUserAggregateId(orderUserDto.getAggregateId());
        setUserVersion(orderUserDto.getVersion());
        setUserState(orderUserDto.getState() != null ? AggregateState.valueOf(orderUserDto.getState()) : null);
    }

    public OrderUser(OrderUser other) {
        setUserAggregateId(other.getUserAggregateId());
        setUserVersion(other.getUserVersion());
        setUserState(other.getUserState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderUserDto buildDto() {
        OrderUserDto dto = new OrderUserDto();
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}