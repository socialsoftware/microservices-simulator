package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.USER_ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.USER_NOT_ACTIVE;

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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserById(Integer aggregateId, UnitOfWork unitOfWork) {
        return userFactory.createUserDto((User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    /*simple user creation*/
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void activateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        if (oldUser.isActive()) {
            throw new QuizzesException(USER_ACTIVE);
        }
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setActive(true);
        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deactivateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        if (!oldUser.isActive()) {
            throw new QuizzesException(USER_NOT_ACTIVE);
        }
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.setActive(false);
        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserFromExisting(oldUser);
        newUser.remove();
        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteUserEvent(newUser.getAggregateId()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
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

