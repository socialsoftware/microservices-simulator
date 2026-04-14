package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Room extends Aggregate {
    private String roomNumber;
    private String description;
    private Double pricePerNight;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "room")
    private Set<RoomAmenity> amenities = new HashSet<>();
    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    public Room() {

    }

    public Room(Integer aggregateId, RoomDto roomDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setRoomNumber(roomDto.getRoomNumber());
        setDescription(roomDto.getDescription());
        setPricePerNight(roomDto.getPricePerNight());
        setStatus(RoomStatus.valueOf(roomDto.getStatus()));
        setAmenities(roomDto.getAmenities() != null ? roomDto.getAmenities().stream().map(RoomAmenity::new).collect(Collectors.toSet()) : null);
    }


    public Room(Room other) {
        super(other);
        setRoomNumber(other.getRoomNumber());
        setDescription(other.getDescription());
        setPricePerNight(other.getPricePerNight());
        setAmenities(other.getAmenities() != null ? other.getAmenities().stream().map(RoomAmenity::new).collect(Collectors.toSet()) : null);
        setStatus(other.getStatus());
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

    public Set<RoomAmenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<RoomAmenity> amenities) {
        this.amenities = amenities;
        if (this.amenities != null) {
            this.amenities.forEach(item -> item.setRoom(this));
        }
    }

    public void addRoomAmenity(RoomAmenity roomAmenity) {
        if (this.amenities == null) {
            this.amenities = new HashSet<>();
        }
        this.amenities.add(roomAmenity);
        if (roomAmenity != null) {
            roomAmenity.setRoom(this);
        }
    }

    public void removeRoomAmenity(Integer id) {
        if (this.amenities != null) {
            this.amenities.removeIf(item -> 
                item.getCode() != null && item.getCode().equals(id));
        }
    }

    public boolean containsRoomAmenity(Integer id) {
        if (this.amenities == null) {
            return false;
        }
        return this.amenities.stream().anyMatch(item -> 
            item.getCode() != null && item.getCode().equals(id));
    }

    public RoomAmenity findRoomAmenityById(Integer id) {
        if (this.amenities == null) {
            return null;
        }
        return this.amenities.stream()
            .filter(item -> item.getCode() != null && item.getCode().equals(id))
            .findFirst()
            .orElse(null);
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.roomNumber != null && this.roomNumber.length() > 0;
    }

    private boolean invariantRule1() {
        return pricePerNight > 0.0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Room number cannot be empty");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Price per night must be positive");
        }
    }

    public RoomDto buildDto() {
        RoomDto dto = new RoomDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setRoomNumber(getRoomNumber());
        dto.setDescription(getDescription());
        dto.setPricePerNight(getPricePerNight());
        dto.setAmenities(getAmenities() != null ? getAmenities().stream().map(RoomAmenity::buildDto).collect(Collectors.toSet()) : null);
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        return dto;
    }
}