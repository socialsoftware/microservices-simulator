package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;

public class CreateCategoryRequestDto {
    @NotNull
    private String name;
    @NotNull
    private String description;

    public CreateCategoryRequestDto() {}

    public CreateCategoryRequestDto(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
