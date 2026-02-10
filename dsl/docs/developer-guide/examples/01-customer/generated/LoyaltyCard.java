package com.generated.abstractions.microservices.customer.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import com.generated.abstractions.shared.dtos.LoyaltyCardDto;

@Entity
public class LoyaltyCard {
    @Id
    @GeneratedValue
    private Long id;
    private String cardNumber;
    private Integer points;
    private LocalDateTime expiresAt;
    @OneToOne
    private Customer customer;

    public LoyaltyCard() {

    }

    public LoyaltyCard(LoyaltyCardDto loyaltyCardDto) {
        setCardNumber(loyaltyCardDto.getCardNumber());
        setPoints(loyaltyCardDto.getPoints());
        setExpiresAt(loyaltyCardDto.getExpiresAt());
    }


    public LoyaltyCard(LoyaltyCard other) {
        setPoints(other.getPoints());
        setExpiresAt(other.getExpiresAt());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }




    public LoyaltyCardDto buildDto() {
        LoyaltyCardDto dto = new LoyaltyCardDto();
        dto.setCardNumber(getCardNumber());
        dto.setPoints(getPoints());
        dto.setExpiresAt(getExpiresAt());
        return dto;
    }
}