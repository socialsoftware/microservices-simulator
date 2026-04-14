package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe.CartSubscribesUserDeletedCartUserExists;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.events.subscribe.CartSubscribesUserUpdated;

import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartUserDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Cart extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "cart")
    private CartUser user;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "cart")
    private Set<CartItem> items = new HashSet<>();
    private Double totalInCents;
    private Integer itemCount;
    private Boolean checkedOut;

    public Cart() {

    }

    public Cart(Integer aggregateId, CartDto cartDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTotalInCents(cartDto.getTotalInCents());
        setItemCount(cartDto.getItemCount());
        setCheckedOut(cartDto.getCheckedOut());
        setUser(cartDto.getUser() != null ? new CartUser(cartDto.getUser()) : null);
        setItems(cartDto.getItems() != null ? cartDto.getItems().stream().map(CartItem::new).collect(Collectors.toSet()) : null);
    }


    public Cart(Cart other) {
        super(other);
        setUser(other.getUser() != null ? new CartUser(other.getUser()) : null);
        setItems(other.getItems() != null ? other.getItems().stream().map(CartItem::new).collect(Collectors.toSet()) : null);
        setTotalInCents(other.getTotalInCents());
        setItemCount(other.getItemCount());
        setCheckedOut(other.getCheckedOut());
    }

    public CartUser getUser() {
        return user;
    }

    public void setUser(CartUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setCart(this);
        }
    }

    public Set<CartItem> getItems() {
        return items;
    }

    public void setItems(Set<CartItem> items) {
        this.items = items;
        if (this.items != null) {
            this.items.forEach(item -> item.setCart(this));
        }
    }

    public void addCartItem(CartItem cartItem) {
        if (this.items == null) {
            this.items = new HashSet<>();
        }
        this.items.add(cartItem);
        if (cartItem != null) {
            cartItem.setCart(this);
        }
    }

    public void removeCartItem(Long id) {
        if (this.items != null) {
            this.items.removeIf(item -> 
                item.getProductId() != null && item.getProductId().equals(id));
        }
    }

    public boolean containsCartItem(Long id) {
        if (this.items == null) {
            return false;
        }
        return this.items.stream().anyMatch(item -> 
            item.getProductId() != null && item.getProductId().equals(id));
    }

    public CartItem findCartItemById(Long id) {
        if (this.items == null) {
            return null;
        }
        return this.items.stream()
            .filter(item -> item.getProductId() != null && item.getProductId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public Double getTotalInCents() {
        return totalInCents;
    }

    public void setTotalInCents(Double totalInCents) {
        this.totalInCents = totalInCents;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Boolean getCheckedOut() {
        return checkedOut;
    }

    public void setCheckedOut(Boolean checkedOut) {
        this.checkedOut = checkedOut;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCartUserExists(eventSubscriptions);
            eventSubscriptions.add(new CartSubscribesUserUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantCartUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new CartSubscribesUserDeletedCartUserExists(this.getUser()));
    }


    private boolean invariantRule0() {
        return totalInCents >= 0.0;
    }

    private boolean invariantRule1() {
        return itemCount >= 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Cart total cannot be negative");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Cart item count cannot be negative");
        }
    }

    public CartDto buildDto() {
        CartDto dto = new CartDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setUser(getUser() != null ? new CartUserDto(getUser()) : null);
        dto.setItems(getItems() != null ? getItems().stream().map(CartItem::buildDto).collect(Collectors.toSet()) : null);
        dto.setTotalInCents(getTotalInCents());
        dto.setItemCount(getItemCount());
        dto.setCheckedOut(getCheckedOut());
        return dto;
    }
}