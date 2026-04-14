package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRepository;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;


@Service
@Transactional(noRollbackFor = ShowcaseException.class)
public class UserService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    @Autowired
    private UserServiceExtension extension;

    @Autowired
    private ApplicationContext applicationContext;

    public UserService() {}

    public UserDto createUser(CreateUserRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            UserDto userDto = new UserDto();
            userDto.setUsername(createRequest.getUsername());
            userDto.setEmail(createRequest.getEmail());
            userDto.setLoyaltyPoints(createRequest.getLoyaltyPoints());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            User user = userFactory.createUser(aggregateId, userDto);
            unitOfWorkService.registerChanged(user, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
        try {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving user: " + e.getMessage());
        }
    }

    public List<UserDto> getAllUsers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = userRepository.findAll().stream()
                .map(User::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(userFactory::createUserDto)
                .collect(Collectors.toList());
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error retrieving user: " + e.getMessage());
        }
    }

    public UserDto updateUser(UserDto userDto, UnitOfWork unitOfWork) {
        try {
            Integer id = userDto.getAggregateId();
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            if (userDto.getUsername() != null) {
                newUser.setUsername(userDto.getUsername());
            }
            if (userDto.getEmail() != null) {
                newUser.setEmail(userDto.getEmail());
            }
            if (userDto.getLoyaltyPoints() != null) {
                newUser.setLoyaltyPoints(userDto.getLoyaltyPoints());
            }

            unitOfWorkService.registerChanged(newUser, unitOfWork);            UserUpdatedEvent event = new UserUpdatedEvent(newUser.getAggregateId(), newUser.getUsername(), newUser.getEmail(), newUser.getLoyaltyPoints());
            event.setPublisherAggregateVersion(newUser.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return userFactory.createUserDto(newUser);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(Integer id, UnitOfWork unitOfWork) {
        try {
            BookingRepository bookingRepositoryRef = applicationContext.getBean(BookingRepository.class);
            boolean hasBookingReferences = bookingRepositoryRef.findAll().stream()
                .filter(s -> s.getState() != User.AggregateState.DELETED)
                .anyMatch(s -> s.getUser() != null && id.equals(s.getUser().getUserAggregateId()));
            if (hasBookingReferences) {
                throw new ShowcaseException("Cannot delete user that has bookings");
            }
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            newUser.remove();
            unitOfWorkService.registerChanged(newUser, unitOfWork);            unitOfWorkService.registerEvent(new UserDeletedEvent(newUser.getAggregateId()), unitOfWork);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error deleting user: " + e.getMessage());
        }
    }





    @Transactional
    public UserDto signUp(String username, String email, UnitOfWork unitOfWork) {
        try {
        UserDto dto = new UserDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setLoyaltyPoints(0);
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, dto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
        } catch (ShowcaseException e) {
            throw e;
        } catch (Exception e) {
            throw new ShowcaseException("Error in signUp User: " + e.getMessage());
        }
    }


}