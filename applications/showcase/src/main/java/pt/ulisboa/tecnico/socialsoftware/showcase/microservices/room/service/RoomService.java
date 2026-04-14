package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.enums.RoomStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.RoomDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.RoomUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.RoomAmenityRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.RoomAmenityUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRepository;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;


@Service
@Transactional(noRollbackFor = ShowcaseException.class)
public class RoomService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomFactory roomFactory;

    @Autowired
    private RoomServiceExtension extension;

    @Autowired
    private ApplicationContext applicationContext;

    public RoomService() {}

    public RoomDto createRoom(CreateRoomRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomNumber(createRequest.getRoomNumber());
            roomDto.setDescription(createRequest.getDescription());
            roomDto.setPricePerNight(createRequest.getPricePerNight());
            roomDto.setStatus(createRequest.getStatus() != null ? createRequest.getStatus().name() : null);
            roomDto.setAmenities(createRequest.getAmenities());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Room room = roomFactory.createRoom(aggregateId, roomDto);
            unitOfWorkService.registerChanged(room, unitOfWork);
            return roomFactory.createRoomDto(room);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error creating room: " + e.getMessage());
        }
    }

    public RoomDto getRoomById(Integer id, UnitOfWork unitOfWork) {
        try {
            Room room = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return roomFactory.createRoomDto(room);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving room: " + e.getMessage());
        }
    }

    public List<RoomDto> getAllRooms(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = roomRepository.findAll().stream()
                .map(Room::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Room) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(roomFactory::createRoomDto)
                .collect(Collectors.toList());
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving room: " + e.getMessage());
        }
    }

    public RoomDto updateRoom(RoomDto roomDto, UnitOfWork unitOfWork) {
        try {
            Integer id = roomDto.getAggregateId();
            Room oldRoom = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Room newRoom = roomFactory.createRoomFromExisting(oldRoom);
            if (roomDto.getRoomNumber() != null) {
                newRoom.setRoomNumber(roomDto.getRoomNumber());
            }
            if (roomDto.getDescription() != null) {
                newRoom.setDescription(roomDto.getDescription());
            }
            if (roomDto.getPricePerNight() != null) {
                newRoom.setPricePerNight(roomDto.getPricePerNight());
            }
            if (roomDto.getStatus() != null) {
                newRoom.setStatus(RoomStatus.valueOf(roomDto.getStatus()));
            }

            unitOfWorkService.registerChanged(newRoom, unitOfWork);            RoomUpdatedEvent event = new RoomUpdatedEvent(newRoom.getAggregateId(), newRoom.getRoomNumber(), newRoom.getDescription(), newRoom.getPricePerNight());
            event.setPublisherAggregateVersion(newRoom.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return roomFactory.createRoomDto(newRoom);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error updating room: " + e.getMessage());
        }
    }

    public void deleteRoom(Integer id, UnitOfWork unitOfWork) {
        try {
            BookingRepository bookingRepositoryRef = applicationContext.getBean(BookingRepository.class);
            boolean hasBookingReferences = bookingRepositoryRef.findAll().stream()
                .collect(Collectors.groupingBy(
                    Booking::getAggregateId,
                    Collectors.maxBy(Comparator.comparingInt(Booking::getVersion))))
                .values().stream()
                .flatMap(Optional::stream)
                .filter(s -> s.getState() != Room.AggregateState.DELETED)
                .anyMatch(s -> s.getRoom() != null && id.equals(s.getRoom().getRoomAggregateId()));
            if (hasBookingReferences) {
                throw new ShowcaseException("Cannot delete room that has bookings");
            }
            Room oldRoom = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Room newRoom = roomFactory.createRoomFromExisting(oldRoom);
            newRoom.remove();
            unitOfWorkService.registerChanged(newRoom, unitOfWork);            unitOfWorkService.registerEvent(new RoomDeletedEvent(newRoom.getAggregateId()), unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error deleting room: " + e.getMessage());
        }
    }

    public RoomAmenityDto addRoomAmenity(Integer roomId, Integer code, RoomAmenityDto RoomAmenityDto, UnitOfWork unitOfWork) {
        try {
            Room oldRoom = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
            Room newRoom = roomFactory.createRoomFromExisting(oldRoom);
            RoomAmenity element = new RoomAmenity(RoomAmenityDto);
            element.setRoom(newRoom);
            newRoom.getAmenities().add(element);
            unitOfWorkService.registerChanged(newRoom, unitOfWork);
            return RoomAmenityDto;
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error adding RoomAmenity: " + e.getMessage());
        }
    }

    public List<RoomAmenityDto> addRoomAmenitys(Integer roomId, List<RoomAmenityDto> RoomAmenityDtos, UnitOfWork unitOfWork) {
        try {
            Room oldRoom = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
            Room newRoom = roomFactory.createRoomFromExisting(oldRoom);
            RoomAmenityDtos.forEach(dto -> {
                RoomAmenity element = new RoomAmenity(dto);
                element.setRoom(newRoom);
                newRoom.getAmenities().add(element);
            });
            unitOfWorkService.registerChanged(newRoom, unitOfWork);
            return RoomAmenityDtos;
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error adding RoomAmenitys: " + e.getMessage());
        }
    }

    public RoomAmenityDto getRoomAmenity(Integer roomId, Integer code, UnitOfWork unitOfWork) {
        try {
            Room room = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
            RoomAmenity element = room.getAmenities().stream()
                .filter(item -> item.getCode() != null &&
                               item.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new ShowcaseException("RoomAmenity not found"));
            return element.buildDto();
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving RoomAmenity: " + e.getMessage());
        }
    }

    public void removeRoomAmenity(Integer roomId, Integer code, UnitOfWork unitOfWork) {
        try {
            Room oldRoom = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
            Room newRoom = roomFactory.createRoomFromExisting(oldRoom);
            newRoom.getAmenities().removeIf(item ->
                item.getCode() != null &&
                item.getCode().equals(code)
            );
            unitOfWorkService.registerChanged(newRoom, unitOfWork);
            RoomAmenityRemovedEvent event = new RoomAmenityRemovedEvent(roomId, code);
            event.setPublisherAggregateVersion(newRoom.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error removing RoomAmenity: " + e.getMessage());
        }
    }

    public RoomAmenityDto updateRoomAmenity(Integer roomId, Integer code, RoomAmenityDto RoomAmenityDto, UnitOfWork unitOfWork) {
        try {
            Room oldRoom = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
            Room newRoom = roomFactory.createRoomFromExisting(oldRoom);
            RoomAmenity element = newRoom.getAmenities().stream()
                .filter(item -> item.getCode() != null &&
                               item.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new ShowcaseException("RoomAmenity not found"));
            if (RoomAmenityDto.getName() != null) {
                element.setName(RoomAmenityDto.getName());
            }
            if (RoomAmenityDto.getDescription() != null) {
                element.setDescription(RoomAmenityDto.getDescription());
            }
            unitOfWorkService.registerChanged(newRoom, unitOfWork);
            RoomAmenityUpdatedEvent event = new RoomAmenityUpdatedEvent(roomId, code);
            event.setPublisherAggregateVersion(newRoom.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error updating RoomAmenity: " + e.getMessage());
        }
    }



    @Transactional
    public void reserve(Integer roomId, UnitOfWork unitOfWork) {
        try {
        Room roomOld = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
        Room room = roomFactory.createRoomFromExisting(roomOld);
        room.setStatus(RoomStatus.RESERVED);
        unitOfWorkService.registerChanged(room, unitOfWork);
        RoomReservedEvent event0 = new RoomReservedEvent();
        event0.setRoomNumber(room.getRoomNumber());
        unitOfWorkService.registerEvent(event0, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in reserve Room: " + e.getMessage());
        }
    }

    @Transactional
    public void checkIn(Integer roomId, UnitOfWork unitOfWork) {
        try {
        Room roomOld = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
        Room room = roomFactory.createRoomFromExisting(roomOld);
        room.setStatus(RoomStatus.OCCUPIED);
        unitOfWorkService.registerChanged(room, unitOfWork);
        RoomOccupiedEvent event0 = new RoomOccupiedEvent();
        event0.setRoomNumber(room.getRoomNumber());
        unitOfWorkService.registerEvent(event0, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in checkIn Room: " + e.getMessage());
        }
    }

    @Transactional
    public void checkOut(Integer roomId, UnitOfWork unitOfWork) {
        try {
        Room roomOld = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
        Room room = roomFactory.createRoomFromExisting(roomOld);
        room.setStatus(RoomStatus.AVAILABLE);
        unitOfWorkService.registerChanged(room, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in checkOut Room: " + e.getMessage());
        }
    }

    @Transactional
    public void release(Integer roomId, UnitOfWork unitOfWork) {
        try {
        Room roomOld = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
        Room room = roomFactory.createRoomFromExisting(roomOld);
        room.setStatus(RoomStatus.AVAILABLE);
        unitOfWorkService.registerChanged(room, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in release Room: " + e.getMessage());
        }
    }

    @Transactional
    public void retire(Integer roomId, UnitOfWork unitOfWork) {
        try {
        Room roomOld = (Room) unitOfWorkService.aggregateLoadAndRegisterRead(roomId, unitOfWork);
        Room room = roomFactory.createRoomFromExisting(roomOld);
        room.setStatus(RoomStatus.OUT_OF_SERVICE);
        unitOfWorkService.registerChanged(room, unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in retire Room: " + e.getMessage());
        }
    }


}