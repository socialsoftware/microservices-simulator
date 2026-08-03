package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
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

    public UserService(UserCustomRepository userCustomRepository,
                       UserFactory userFactory,
                       UnitOfWorkService unitOfWorkService) {
        this.userCustomRepository = userCustomRepository;
        this.userFactory = userFactory;
        this.unitOfWorkService = unitOfWorkService;
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
}
