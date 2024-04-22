package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalUser;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.events.publish.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.Role;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

@Service
public class UserService {
    
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UserRepository userRepository;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private UserFactory userFactory;
    
    public UserService(UnitOfWorkService unitOfWorkService, UserRepository userRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.userRepository = userRepository;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserDto getUserById(Integer aggregateId, UnitOfWork unitOfWork) {
        return new UserDto((User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    /*simple user creation*/
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);
        unitOfWork.registerChanged(user);
        return new UserDto(user);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void activateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        if (oldUser.isActive()) {
            throw new TutorException(USER_ACTIVE);
        }
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setActive(true);
        unitOfWork.registerChanged(newUser);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWork.registerChanged(newUser);
        unitOfWork.addEvent(new DeleteUserEvent(newUser.getAggregateId()));
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<UserDto> getStudents(UnitOfWork unitOfWork) {
        Set<Integer> studentsIds = userRepository.findAll().stream()
                .filter(u -> u.getRole().equals(Role.STUDENT))
                .map(User::getAggregateId)
                .collect(Collectors.toSet());
        return studentsIds.stream()
                .map(id -> (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<UserDto> getTeachers(UnitOfWork unitOfWork) {
        Set<Integer> teacherIds = userRepository.findAll().stream()
                .filter(u -> u.getRole().equals(Role.TEACHER))
                .map(User::getAggregateId)
                .collect(Collectors.toSet());
        return teacherIds.stream()
                .map(id -> (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(UserDto::new)
                .collect(Collectors.toList());
    }
}
