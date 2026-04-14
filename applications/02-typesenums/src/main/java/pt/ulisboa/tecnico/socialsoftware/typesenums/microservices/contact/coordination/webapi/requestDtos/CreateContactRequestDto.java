package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.enums.ContactCategory;
import java.time.LocalDateTime;

public class CreateContactRequestDto {
    @NotNull
    private String firstName;
    @NotNull
    private String lastName;
    @NotNull
    private String email;
    @NotNull
    private ContactCategory category;
    @NotNull
    private LocalDateTime createdAt;
    @NotNull
    private Boolean favorite;
    @NotNull
    private Integer callCount;

    public CreateContactRequestDto() {}

    public CreateContactRequestDto(String firstName, String lastName, String email, ContactCategory category, LocalDateTime createdAt, Boolean favorite, Integer callCount) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.category = category;
        this.createdAt = createdAt;
        this.favorite = favorite;
        this.callCount = callCount;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public ContactCategory getCategory() {
        return category;
    }

    public void setCategory(ContactCategory category) {
        this.category = category;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public Boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(Boolean favorite) {
        this.favorite = favorite;
    }
    public Integer getCallCount() {
        return callCount;
    }

    public void setCallCount(Integer callCount) {
        this.callCount = callCount;
    }
}
