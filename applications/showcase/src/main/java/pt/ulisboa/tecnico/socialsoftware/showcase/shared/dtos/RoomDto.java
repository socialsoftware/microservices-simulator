package pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.Room;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.RoomAmenity;

public class RoomDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String roomNumber;
    private String description;
    private Double pricePerNight;
    private Set<RoomAmenityDto> amenities;
    private String status;

    public RoomDto() {
    }

    public RoomDto(Room room) {
        this.aggregateId = room.getAggregateId();
        this.version = room.getVersion();
        this.state = room.getState();
        this.roomNumber = room.getRoomNumber();
        this.description = room.getDescription();
        this.pricePerNight = room.getPricePerNight();
        this.amenities = room.getAmenities() != null ? room.getAmenities().stream().map(RoomAmenity::buildDto).collect(Collectors.toSet()) : null;
        this.status = room.getStatus() != null ? room.getStatus().name() : null;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}