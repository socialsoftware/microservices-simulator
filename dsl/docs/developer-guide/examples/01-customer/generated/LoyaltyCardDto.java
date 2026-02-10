package com.generated.abstractions.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import com.generated.abstractions.microservices.customer.aggregate.LoyaltyCard;

public class LoyaltyCardDto implements Serializable {
    private String cardNumber;
    private Integer points;
    private LocalDateTime expiresAt;

    public LoyaltyCardDto() {
    }

    public LoyaltyCardDto(LoyaltyCard loyaltyCard) {
        this.cardNumber = loyaltyCard.getCardNumber();
        this.points = loyaltyCard.getPoints();
        this.expiresAt = loyaltyCard.getExpiresAt();
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
}