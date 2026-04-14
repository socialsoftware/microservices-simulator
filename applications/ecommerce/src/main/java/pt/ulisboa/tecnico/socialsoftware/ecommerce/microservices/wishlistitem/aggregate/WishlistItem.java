package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.subscribe.WishlistItemSubscribesProductUpdated;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.events.subscribe.WishlistItemSubscribesUserUpdated;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemProductDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemUserDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class WishlistItem extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "wishlistItem")
    private WishlistItemUser user;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "wishlistItem")
    private WishlistItemProduct product;
    private String addedAt;

    public WishlistItem() {

    }

    public WishlistItem(Integer aggregateId, WishlistItemDto wishlistItemDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAddedAt(wishlistItemDto.getAddedAt());
        setUser(wishlistItemDto.getUser() != null ? new WishlistItemUser(wishlistItemDto.getUser()) : null);
        setProduct(wishlistItemDto.getProduct() != null ? new WishlistItemProduct(wishlistItemDto.getProduct()) : null);
    }


    public WishlistItem(WishlistItem other) {
        super(other);
        setUser(other.getUser() != null ? new WishlistItemUser(other.getUser()) : null);
        setProduct(other.getProduct() != null ? new WishlistItemProduct(other.getProduct()) : null);
        setAddedAt(other.getAddedAt());
    }

    public WishlistItemUser getUser() {
        return user;
    }

    public void setUser(WishlistItemUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setWishlistItem(this);
        }
    }

    public WishlistItemProduct getProduct() {
        return product;
    }

    public void setProduct(WishlistItemProduct product) {
        this.product = product;
        if (this.product != null) {
            this.product.setWishlistItem(this);
        }
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            eventSubscriptions.add(new WishlistItemSubscribesUserUpdated());
            eventSubscriptions.add(new WishlistItemSubscribesProductUpdated());
        }
        return eventSubscriptions;
    }



    private boolean invariantRule0() {
        return this.addedAt != null && this.addedAt.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Wishlist item must have an added timestamp");
        }
    }

    public WishlistItemDto buildDto() {
        WishlistItemDto dto = new WishlistItemDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUser(getUser() != null ? new WishlistItemUserDto(getUser()) : null);
        dto.setProduct(getProduct() != null ? new WishlistItemProductDto(getProduct()) : null);
        dto.setAddedAt(getAddedAt());
        return dto;
    }
}