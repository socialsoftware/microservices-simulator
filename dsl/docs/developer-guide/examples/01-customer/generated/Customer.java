package com.generated.abstractions.microservices.customer.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.generated.ms.domain.aggregate.Aggregate;
import com.generated.ms.domain.event.EventSubscription;

import com.generated.abstractions.shared.dtos.AddressDto;
import com.generated.abstractions.shared.dtos.CustomerDto;

@Entity
public abstract class Customer extends Aggregate {
    private String name;
    private String email;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "customer")
    private Address address;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "customer")
    private Set<LoyaltyCard> loyaltyCards = new HashSet<>();

    public Customer() {

    }

    public Customer(Integer aggregateId, CustomerDto customerDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(customerDto.getName());
        setEmail(customerDto.getEmail());
        setAddress(customerDto.getAddress() != null ? new Address(customerDto.getAddress()) : null);
        setLoyaltyCards(customerDto.getLoyaltyCards() != null ? customerDto.getLoyaltyCards().stream().map(LoyaltyCard::new).collect(Collectors.toSet()) : null);
    }


    public Customer(Customer other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());
        setAddress(new Address(other.getAddress()));
        setLoyaltyCards(other.getLoyaltyCards().stream().map(LoyaltyCard::new).collect(Collectors.toSet()));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
        if (this.address != null) {
            this.address.setCustomer(this);
        }
    }

    public Set<LoyaltyCard> getLoyaltyCards() {
        return loyaltyCards;
    }

    public void setLoyaltyCards(Set<LoyaltyCard> loyaltyCards) {
        this.loyaltyCards = loyaltyCards;
        if (this.loyaltyCards != null) {
            this.loyaltyCards.forEach(item -> item.setCustomer(this));
        }
    }

    public void addLoyaltyCard(LoyaltyCard loyaltyCard) {
        if (this.loyaltyCards == null) {
            this.loyaltyCards = new HashSet<>();
        }
        this.loyaltyCards.add(loyaltyCard);
        if (loyaltyCard != null) {
            loyaltyCard.setCustomer(this);
        }
    }

    public void removeLoyaltyCard(String id) {
        if (this.loyaltyCards != null) {
            this.loyaltyCards.removeIf(item -> 
                item.getCardNumber() != null && item.getCardNumber().equals(id));
        }
    }

    public boolean containsLoyaltyCard(String id) {
        if (this.loyaltyCards == null) {
            return false;
        }
        return this.loyaltyCards.stream().anyMatch(item -> 
            item.getCardNumber() != null && item.getCardNumber().equals(id));
    }

    public LoyaltyCard findLoyaltyCardById(String id) {
        if (this.loyaltyCards == null) {
            return null;
        }
        return this.loyaltyCards.stream()
            .filter(item -> item.getCardNumber() != null && item.getCardNumber().equals(id))
            .findFirst()
            .orElse(null);
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

    public CustomerDto buildDto() {
        CustomerDto dto = new CustomerDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setEmail(getEmail());
        dto.setAddress(getAddress() != null ? new AddressDto(getAddress()) : null);
        dto.setLoyaltyCards(getLoyaltyCards() != null ? getLoyaltyCards().stream().map(LoyaltyCard::buildDto).collect(Collectors.toSet()) : null);
        return dto;
    }
}