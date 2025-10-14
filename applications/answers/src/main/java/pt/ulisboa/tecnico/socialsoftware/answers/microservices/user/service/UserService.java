package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    public UserService() {}

    // CRUD Operations
    public UserDto createUser(String name, String username, Boolean active) {
        try {
            User user = new User(name, username, active);
            user = userRepository.save(user);
            return new UserDto(user);
        } catch (Exception e) {
            throw new AnswersException("Error creating user: " + e.getMessage());
        }
    }

    public UserDto getUserById(Integer id) {
        try {
            User user = (User) userRepository.findById(id)
                .orElseThrow(() -> new AnswersException("User not found with id: " + id));
            return new UserDto(user);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving user: " + e.getMessage());
        }
    }

    public List<UserDto> getAllUsers() {
        try {
            return userRepository.findAll().stream()
                .map(entity -> new UserDto((User) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all users: " + e.getMessage());
        }
    }

    public UserDto updateUser(Integer id, UserDto userDto) {
        try {
            User user = (User) userRepository.findById(id)
                .orElseThrow(() -> new AnswersException("User not found with id: " + id));
            
                        if (userDto.getName() != null) {
                user.setName(userDto.getName());
            }
            if (userDto.getUsername() != null) {
                user.setUsername(userDto.getUsername());
            }
            user.setActive(userDto.isActive());
            
            user = userRepository.save(user);
            return new UserDto(user);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(Integer id) {
        try {
            if (!userRepository.existsById(id)) {
                throw new AnswersException("User not found with id: " + id);
            }
            userRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting user: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented

    // Event Processing Methods
    private void publishUserCreatedEvent(User user) {
        try {
            // TODO: Implement event publishing for UserCreated
            // eventPublisher.publishEvent(new UserCreatedEvent(user));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish UserCreatedEvent", e);
        }
    }

    private void publishUserUpdatedEvent(User user) {
        try {
            // TODO: Implement event publishing for UserUpdated
            // eventPublisher.publishEvent(new UserUpdatedEvent(user));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish UserUpdatedEvent", e);
        }
    }

    private void publishUserDeletedEvent(Long userId) {
        try {
            // TODO: Implement event publishing for UserDeleted
            // eventPublisher.publishEvent(new UserDeletedEvent(userId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish UserDeletedEvent", e);
        }
    }
}