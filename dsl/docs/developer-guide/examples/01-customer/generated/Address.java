package com.generated.abstractions.microservices.customer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import com.generated.abstractions.shared.dtos.AddressDto;

@Entity
public class Address {
    @Id
    @GeneratedValue
    private Long id;
    private String street;
    private String city;
    private String zipCode;
    @OneToOne
    private Customer customer;

    public Address() {

    }

    public Address(AddressDto addressDto) {
        setStreet(addressDto.getStreet());
        setCity(addressDto.getCity());
        setZipCode(addressDto.getZipCode());
    }


    public Address(Address other) {
        setCity(other.getCity());
        setZipCode(other.getZipCode());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }




    public AddressDto buildDto() {
        AddressDto dto = new AddressDto();
        dto.setStreet(getStreet());
        dto.setCity(getCity());
        dto.setZipCode(getZipCode());
        return dto;
    }
}