package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;

public class CreateOrderRequestDto {
    @NotNull
    private UserDto user;
    @NotNull
    private String time;
    @NotNull
    private Double totalPriceInCents;
    @NotNull
    private String addressName;
    @NotNull
    private String address1;
    @NotNull
    private String address2;
    @NotNull
    private String creditCardCompany;
    @NotNull
    private String creditCardNumber;
    @NotNull
    private String creditCardExpiryDate;

    public CreateOrderRequestDto() {}

    public CreateOrderRequestDto(UserDto user, String time, Double totalPriceInCents, String addressName, String address1, String address2, String creditCardCompany, String creditCardNumber, String creditCardExpiryDate) {
        this.user = user;
        this.time = time;
        this.totalPriceInCents = totalPriceInCents;
        this.addressName = addressName;
        this.address1 = address1;
        this.address2 = address2;
        this.creditCardCompany = creditCardCompany;
        this.creditCardNumber = creditCardNumber;
        this.creditCardExpiryDate = creditCardExpiryDate;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
    public Double getTotalPriceInCents() {
        return totalPriceInCents;
    }

    public void setTotalPriceInCents(Double totalPriceInCents) {
        this.totalPriceInCents = totalPriceInCents;
    }
    public String getAddressName() {
        return addressName;
    }

    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }
    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }
    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }
    public String getCreditCardCompany() {
        return creditCardCompany;
    }

    public void setCreditCardCompany(String creditCardCompany) {
        this.creditCardCompany = creditCardCompany;
    }
    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }
    public String getCreditCardExpiryDate() {
        return creditCardExpiryDate;
    }

    public void setCreditCardExpiryDate(String creditCardExpiryDate) {
        this.creditCardExpiryDate = creditCardExpiryDate;
    }
}
