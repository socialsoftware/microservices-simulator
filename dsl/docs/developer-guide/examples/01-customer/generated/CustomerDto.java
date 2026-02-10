package com.generated.abstractions.shared.dtos;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import com.generated.abstractions.microservices.customer.aggregate.Customer;
import com.generated.abstractions.microservices.customer.aggregate.LoyaltyCard;

public class CustomerDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String email;
    private AddressDto address;
    private Set<LoyaltyCardDto> loyaltyCards;

    public CustomerDto() {
    }

    public CustomerDto(Customer customer) {
        this.aggregateId = customer.getAggregateId();
        this.version = customer.getVersion();
        this.state = customer.getState();
        this.name = customer.getName();
        this.email = customer.getEmail();
        this.address = customer.getAddress() != null ? new AddressDto(customer.getAddress()) : null;
        this.loyaltyCards = customer.getLoyaltyCards() != null ? customer.getLoyaltyCards().stream().map(LoyaltyCard::buildDto).collect(Collectors.toSet()) : null;
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

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public Set<LoyaltyCardDto> getLoyaltyCards() {
        return loyaltyCards;
    }

    public void setLoyaltyCards(Set<LoyaltyCardDto> loyaltyCards) {
        this.loyaltyCards = loyaltyCards;
    }
}