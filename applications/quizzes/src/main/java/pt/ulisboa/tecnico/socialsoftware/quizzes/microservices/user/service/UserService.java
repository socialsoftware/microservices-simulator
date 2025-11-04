package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.USER_ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.USER_NOT_ACTIVE;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

@Service
public class UserService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UserRepository userRepository;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final UserTransactionalService userTransactionalService;

    @Autowired
    private UserFactory userFactory;

    public UserService(UnitOfWorkService unitOfWorkService, UserRepository userRepository,
            UserTransactionalService userTransactionalService) {
        this.unitOfWorkService = unitOfWorkService;
        this.userRepository = userRepository;
        this.userTransactionalService = userTransactionalService;
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public UserDto getUserById(Integer aggregateId, UnitOfWork unitOfWork) {
        return userTransactionalService.getUserByIdTransactional(aggregateId, unitOfWork, unitOfWorkService,
                userFactory);
    }

    /* simple user creation */
    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        return userTransactionalService.createUserTransactional(userDto, unitOfWork, aggregateIdGeneratorService,
                userFactory, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void activateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        userTransactionalService.activateUserTransactional(userAggregateId, unitOfWork, unitOfWorkService, userFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void deactivateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        userTransactionalService.deactivateUserTransactional(userAggregateId, unitOfWork, unitOfWorkService,
                userFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void deleteUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        userTransactionalService.deleteUserTransactional(userAggregateId, unitOfWork, unitOfWorkService, userFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public List<UserDto> getStudents(UnitOfWork unitOfWork) {
        return userTransactionalService.getStudentsTransactional(unitOfWork, userRepository, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public List<UserDto> getTeachers(UnitOfWork unitOfWork) {
        return userTransactionalService.getTeachersTransactional(unitOfWork, userRepository, unitOfWorkService);
    }
}

@Service
class UserTransactionalService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserByIdTransactional(Integer aggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, UserFactory userFactory) {
        return userFactory
                .createUserDto((User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUserTransactional(UserDto userDto, UnitOfWork unitOfWork,
            AggregateIdGeneratorService aggregateIdGeneratorService, UserFactory userFactory,
            UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void activateUserTransactional(Integer userAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, UserFactory userFactory) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        if (oldUser.isActive()) {
            throw new QuizzesException(USER_ACTIVE);
        }
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setActive(true);
        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deactivateUserTransactional(Integer userAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, UserFactory userFactory) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        if (!oldUser.isActive()) {
            throw new QuizzesException(USER_NOT_ACTIVE);
        }
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setActive(false);
        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUserTransactional(Integer userAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, UserFactory userFactory) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteUserEvent(newUser.getAggregateId()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getStudentsTransactional(UnitOfWork unitOfWork, UserRepository userRepository,
            UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Set<Integer> studentsIds = userRepository.findAll().stream()
                .filter(u -> u.getRole().equals(Role.STUDENT))
                .map(User::getAggregateId)
                .collect(Collectors.toSet());
        return studentsIds.stream()
                .map(id -> (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(UserDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getTeachersTransactional(UnitOfWork unitOfWork, UserRepository userRepository,
            UnitOfWorkService<UnitOfWork> unitOfWorkService) {
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
