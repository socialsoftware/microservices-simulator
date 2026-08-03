package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.ActivateUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserCustomRepository userCustomRepository;
    private final UserFactory userFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public UserService(UserCustomRepository userCustomRepository,
                       UserFactory userFactory,
                       UnitOfWorkService unitOfWorkService,
                       AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.userCustomRepository = userCustomRepository;
        this.userFactory = userFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserById(Integer userAggregateId, UnitOfWork unitOfWork) {
        return userFactory.createUserDto(
                (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getStudents(UnitOfWork unitOfWork) {
        return getUsersByRole(Role.STUDENT, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getTeachers(UnitOfWork unitOfWork) {
        return getUsersByRole(Role.TEACHER, unitOfWork);
    }

    private List<UserDto> getUsersByRole(Role role, UnitOfWork unitOfWork) {
        return userCustomRepository.findUserIdsByRole(role).stream()
                .map(userAggregateId -> userFactory.createUserDto(
                        (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto.getName(), userDto.getUsername(),
                userDto.getRole());

        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void activateUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserCopy(oldUser);

        newUser.setActive(true);

        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(
                new ActivateUserEvent(newUser.getAggregateId(), newUser.isActive()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateUserName(Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserCopy(oldUser);

        newUser.setName(name);

        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(
                new UpdateStudentNameEvent(newUser.getAggregateId(), newUser.getName()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void anonymizeUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserCopy(oldUser);

        newUser.setName(User.ANONYMOUS);
        newUser.setUsername(User.ANONYMOUS);

        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(
                new AnonymizeStudentEvent(newUser.getAggregateId(), newUser.getName(), newUser.getUsername()),
                unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserCopy(oldUser);

        newUser.setActive(false);
        newUser.remove();

        unitOfWorkService.registerChanged(newUser, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteUserEvent(newUser.getAggregateId()), unitOfWork);
    }
}
