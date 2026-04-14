package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus;

public class CreateRoomRequestDto {
    @NotNull
    private String roomNumber;
    @NotNull
    private String description;
    @NotNull
    private Double pricePerNight;
    private Set<RoomAmenityDto> amenities;
    @NotNull
    private RoomStatus status;

    public CreateRoomRequestDto() {}

    public CreateRoomRequestDto(String roomNumber, String description, Double pricePerNight, Set<RoomAmenityDto> amenities, RoomStatus status) {
        this.roomNumber = roomNumber;
        this.description = description;
        this.pricePerNight = pricePerNight;
        this.amenities = amenities;
        this.status = status;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public Double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }
    public Set<RoomAmenityDto> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<RoomAmenityDto> amenities) {
        this.amenities = amenities;
    }
    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
