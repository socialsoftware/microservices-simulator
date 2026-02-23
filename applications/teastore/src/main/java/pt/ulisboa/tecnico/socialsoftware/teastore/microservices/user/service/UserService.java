package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import pt.ulisboa.tecnico.socialsoftware.teastore.coordination.webapi.requestDtos.CreateUserRequestDto;


@Service
@Transactional
public class UserService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    public UserService() {}

    public UserDto createUser(CreateUserRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            UserDto userDto = new UserDto();
            userDto.setUserName(createRequest.getUserName());
            userDto.setPassword(createRequest.getPassword());
            userDto.setRealName(createRequest.getRealName());
            userDto.setEmail(createRequest.getEmail());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            User user = userFactory.createUser(aggregateId, userDto);
            unitOfWorkService.registerChanged(user, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
        try {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving user: " + e.getMessage());
        }
    }

    public List<UserDto> getAllUsers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = userRepository.findAll().stream()
                .map(User::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(userFactory::createUserDto)
                .collect(Collectors.toList());
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving user: " + e.getMessage());
        }
    }

    public UserDto updateUser(UserDto userDto, UnitOfWork unitOfWork) {
        try {
            Integer id = userDto.getAggregateId();
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            if (userDto.getUserName() != null) {
                newUser.setUserName(userDto.getUserName());
            }
            if (userDto.getPassword() != null) {
                newUser.setPassword(userDto.getPassword());
            }
            if (userDto.getRealName() != null) {
                newUser.setRealName(userDto.getRealName());
            }
            if (userDto.getEmail() != null) {
                newUser.setEmail(userDto.getEmail());
            }

            unitOfWorkService.registerChanged(newUser, unitOfWork);            UserUpdatedEvent event = new UserUpdatedEvent(newUser.getAggregateId(), newUser.getUserName(), newUser.getPassword(), newUser.getRealName(), newUser.getEmail());
            event.setPublisherAggregateVersion(newUser.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return userFactory.createUserDto(newUser);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(Integer id, UnitOfWork unitOfWork) {
        try {
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            newUser.remove();
            unitOfWorkService.registerChanged(newUser, unitOfWork);            unitOfWorkService.registerEvent(new UserDeletedEvent(newUser.getAggregateId()), unitOfWork);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error deleting user: " + e.getMessage());
        }
    }








}