package com.generated.abstractions.shared.dtos;

import java.io.Serializable;
import com.generated.abstractions.microservices.customer.aggregate.Address;

public class AddressDto implements Serializable {
    private String street;
    private String city;
    private String zipCode;

    public AddressDto() {
    }

    public AddressDto(Address address) {
        this.street = address.getStreet();
        this.city = address.getCity();
        this.zipCode = address.getZipCode();
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}