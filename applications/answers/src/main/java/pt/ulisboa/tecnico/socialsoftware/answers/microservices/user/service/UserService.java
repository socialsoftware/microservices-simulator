package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateUserRequestDto;


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
            userDto.setName(createRequest.getName());
            userDto.setUsername(createRequest.getUsername());
            userDto.setRole(createRequest.getRole() != null ? createRequest.getRole().name() : null);
            userDto.setActive(createRequest.getActive());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            User user = userFactory.createUser(aggregateId, userDto);
            unitOfWorkService.registerChanged(user, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (Exception e) {
            throw new AnswersException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto getUserById(Integer id, UnitOfWork unitOfWork) {
        try {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return userFactory.createUserDto(user);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving user: " + e.getMessage());
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
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all users: " + e.getMessage());
        }
    }

    public UserDto updateUser(UserDto userDto, UnitOfWork unitOfWork) {
        try {
            Integer id = userDto.getAggregateId();
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (userDto.getName() != null) {
                user.setName(userDto.getName());
            }
            if (userDto.getUsername() != null) {
                user.setUsername(userDto.getUsername());
            }
            user.setActive(userDto.getActive());

            unitOfWorkService.registerChanged(user, unitOfWork);
            unitOfWorkService.registerEvent(new UserUpdatedEvent(user.getAggregateId(), user.getName(), user.getUsername(), user.getActive()), unitOfWork);
            return userFactory.createUserDto(user);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(Integer id, UnitOfWork unitOfWork) {
        try {
            User user = (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            user.remove();
            unitOfWorkService.registerChanged(user, unitOfWork);
            unitOfWorkService.registerEvent(new UserDeletedEvent(user.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting user: " + e.getMessage());
        }
    }




}