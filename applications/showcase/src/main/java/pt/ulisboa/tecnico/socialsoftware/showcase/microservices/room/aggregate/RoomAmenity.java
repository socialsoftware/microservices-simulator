package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;

@Entity
public class RoomAmenity {
    @Id
    @GeneratedValue
    private Long id;
    private Integer code;
    private String name;
    private String description;
    @ManyToOne
    private Room room;

    public RoomAmenity() {

    }

    public RoomAmenity(RoomAmenityDto roomAmenityDto) {
        setCode(roomAmenityDto.getCode());
        setName(roomAmenityDto.getName());
        setDescription(roomAmenityDto.getDescription());
    }


    public RoomAmenity(RoomAmenity other) {
        setCode(other.getCode());
        setName(other.getName());
        setDescription(other.getDescription());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }




    public RoomAmenityDto buildDto() {
        RoomAmenityDto dto = new RoomAmenityDto();
        dto.setCode(getCode());
        dto.setName(getName());
        dto.setDescription(getDescription());
        return dto;
    }
}