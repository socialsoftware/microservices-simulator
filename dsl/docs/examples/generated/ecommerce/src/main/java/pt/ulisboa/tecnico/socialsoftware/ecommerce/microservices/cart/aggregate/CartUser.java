package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartUserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;

@Entity
public class CartUser {
    @Id
    @GeneratedValue
    private Long id;
    private String userName;
    private String userEmail;
    private Integer userAggregateId;
    private Integer userVersion;
    private AggregateState userState;
    @OneToOne
    private Cart cart;

    public CartUser() {

    }

    public CartUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public CartUser(CartUserDto cartUserDto) {
        setUserName(cartUserDto.getUsername());
        setUserEmail(cartUserDto.getEmail());
        setUserAggregateId(cartUserDto.getAggregateId());
        setUserVersion(cartUserDto.getVersion());
        setUserState(cartUserDto.getState() != null ? AggregateState.valueOf(cartUserDto.getState()) : null);
    }

    public CartUser(CartUser other) {
        setUserName(other.getUserName());
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

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }




    public CartUserDto buildDto() {
        CartUserDto dto = new CartUserDto();
        dto.setUsername(getUserName());
        dto.setEmail(getUserEmail());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}