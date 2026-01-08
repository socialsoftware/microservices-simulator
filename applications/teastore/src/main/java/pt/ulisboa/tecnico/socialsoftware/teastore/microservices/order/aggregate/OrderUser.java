package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;

@Entity
public class OrderUser {
    @Id
    @GeneratedValue
    private Long id;
    private Integer userAggregateId;
    private String userName;
    private String userRealName;
    private String userEmail;
    @OneToOne
    private Order order;

    public OrderUser() {

    }

    public OrderUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserName(userDto.getUserName());
        setUserRealName(userDto.getRealName());
        setUserEmail(userDto.getEmail());
    }

    public OrderUser(OrderUser other) {
        setUserAggregateId(other.getUserAggregateId());
        setUserName(other.getUserName());
        setUserRealName(other.getUserRealName());
        setUserEmail(other.getUserEmail());
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }


    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getUserAggregateId());
        dto.setUserName(getUserName());
        dto.setRealName(getUserRealName());
        dto.setEmail(getUserEmail());
        return dto;
    }
}