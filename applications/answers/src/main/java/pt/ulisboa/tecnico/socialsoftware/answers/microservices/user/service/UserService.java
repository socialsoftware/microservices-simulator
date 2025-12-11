package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;

@Service
public class UserService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final UserRepository userRepository;

    @Autowired
    private UserFactory userFactory;

    public UserService(UnitOfWorkService unitOfWorkService, UserRepository userRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.userRepository = userRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserById(Integer aggregateId, UnitOfWork unitOfWork) {
        return userFactory.createUserDto((User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto updateUser(Integer aggregateId, UserDto userDto, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setName(userDto.getName());
        newUser.setUsername(userDto.getUsername());
        newUser.setActive(userDto.getActive());
        unitOfWorkService.registerChanged(newUser, unitOfWork);
        return userFactory.createUserDto(newUser);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUser(Integer aggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getAllUsers(UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = userRepository.findAll().stream()
                .map(User::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (User) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(userFactory::createUserDto)
                .collect(Collectors.toList());
    }

}
