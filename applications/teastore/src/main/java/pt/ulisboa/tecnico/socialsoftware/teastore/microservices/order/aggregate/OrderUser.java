package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderUserDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;

@Entity
public class OrderUser {
    @Id
    @GeneratedValue
    private Long id;
    private String userName;
    private String userRealName;
    private String userEmail;
    private Integer userAggregateId;
    private Integer userVersion;
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
        setUserName(orderUserDto.getUserName());
        setUserRealName(orderUserDto.getRealName());
        setUserEmail(orderUserDto.getEmail());
        setUserAggregateId(orderUserDto.getAggregateId());
        setUserVersion(orderUserDto.getVersion());
        setUserState(orderUserDto.getState() != null ? AggregateState.valueOf(orderUserDto.getState()) : null);
    }

    public OrderUser(OrderUser other) {
        setUserName(other.getUserName());
        setUserRealName(other.getUserRealName());
        setUserEmail(other.getUserEmail());
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserRealName() {
        return userRealName;
    }

    public void setUserRealName(String userRealName) {
        this.userRealName = userRealName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Integer userVersion) {
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
        dto.setUserName(getUserName());
        dto.setRealName(getUserRealName());
        dto.setEmail(getUserEmail());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}