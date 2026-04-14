package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemUserDto;

@Entity
public class WishlistItemUser {
    @Id
    @GeneratedValue
    private Long id;
    private String userName;
    private String userEmail;
    private Integer userAggregateId;
    private Integer userVersion;
    private AggregateState userState;
    @OneToOne
    private WishlistItem wishlistItem;

    public WishlistItemUser() {

    }

    public WishlistItemUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserState(userDto.getState());
    }

    public WishlistItemUser(WishlistItemUserDto wishlistItemUserDto) {
        setUserName(wishlistItemUserDto.getUsername());
        setUserEmail(wishlistItemUserDto.getEmail());
        setUserAggregateId(wishlistItemUserDto.getAggregateId());
        setUserVersion(wishlistItemUserDto.getVersion());
        setUserState(wishlistItemUserDto.getState() != null ? AggregateState.valueOf(wishlistItemUserDto.getState()) : null);
    }

    public WishlistItemUser(WishlistItemUser other) {
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

    public WishlistItem getWishlistItem() {
        return wishlistitem;
    }

    public void setWishlistItem(WishlistItem wishlistitem) {
        this.wishlistitem = wishlistitem;
    }




    public WishlistItemUserDto buildDto() {
        WishlistItemUserDto dto = new WishlistItemUserDto();
        dto.setUsername(getUserName());
        dto.setEmail(getUserEmail());
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setState(getUserState() != null ? getUserState().name() : null);
        return dto;
    }
}