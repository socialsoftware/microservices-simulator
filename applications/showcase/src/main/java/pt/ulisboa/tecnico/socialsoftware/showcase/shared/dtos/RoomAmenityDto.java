package pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.RoomAmenity;

public class RoomAmenityDto implements Serializable {
    private Integer code;
    private String name;
    private String description;

    public RoomAmenityDto() {
    }

    public RoomAmenityDto(RoomAmenity roomAmenity) {
        this.code = roomAmenity.getCode();
        this.name = roomAmenity.getName();
        this.description = roomAmenity.getDescription();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
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