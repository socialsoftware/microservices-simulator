package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.Gender;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.DocumentType;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;


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
            userDto.setGender(createRequest.getGender() != null ? createRequest.getGender().name() : null);
            userDto.setDocumentType(createRequest.getDocumentType() != null ? createRequest.getDocumentType().name() : null);
            userDto.setDocumentNum(createRequest.getDocumentNum());
            userDto.setEmail(createRequest.getEmail());
            userDto.setBalance(createRequest.getBalance());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            User user = userFactory.createUser(aggregateId, userDto);
            unitOfWorkService.registerChanged(user, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
        try {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving user: " + e.getMessage());
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
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error retrieving user: " + e.getMessage());
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
            if (userDto.getGender() != null) {
                newUser.setGender(Gender.valueOf(userDto.getGender()));
            }
            if (userDto.getDocumentType() != null) {
                newUser.setDocumentType(DocumentType.valueOf(userDto.getDocumentType()));
            }
            if (userDto.getDocumentNum() != null) {
                newUser.setDocumentNum(userDto.getDocumentNum());
            }
            if (userDto.getEmail() != null) {
                newUser.setEmail(userDto.getEmail());
            }
            if (userDto.getBalance() != null) {
                newUser.setBalance(userDto.getBalance());
            }

            unitOfWorkService.registerChanged(newUser, unitOfWork);            UserUpdatedEvent event = new UserUpdatedEvent(newUser.getAggregateId(), newUser.getUserName(), newUser.getPassword(), newUser.getDocumentNum(), newUser.getEmail(), newUser.getBalance());
            event.setPublisherAggregateVersion(newUser.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return userFactory.createUserDto(newUser);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(Integer id, UnitOfWork unitOfWork) {
        try {
            User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            User newUser = userFactory.createUserFromExisting(oldUser);
            newUser.remove();
            unitOfWorkService.registerChanged(newUser, unitOfWork);            unitOfWorkService.registerEvent(new UserDeletedEvent(newUser.getAggregateId()), unitOfWork);
        } catch (TrainTicketException e) {
            throw e;
        } catch (Exception e) {
            throw new TrainTicketException("Error deleting user: " + e.getMessage());
        }
    }








}