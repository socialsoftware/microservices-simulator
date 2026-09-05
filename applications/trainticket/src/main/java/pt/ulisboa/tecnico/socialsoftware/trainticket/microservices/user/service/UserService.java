package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketException;

import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainticketErrorMessage.DUPLICATE_USER_NAME;

@Service
public class UserService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UserFactory userFactory;

    private final UserRepository userRepository;
    private final UserCustomRepository userCustomRepository;
    private final UnitOfWorkService unitOfWorkService;

    public UserService(UnitOfWorkService unitOfWorkService,
                       UserRepository userRepository,
                       UserCustomRepository userCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.userRepository = userRepository;
        this.userCustomRepository = userCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserById(Integer userAggregateId, UnitOfWork unitOfWork) {
        return userFactory.createUserDto(
                (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<UserDto> getUsers(UnitOfWork unitOfWork) {
        List<UserDto> users = new ArrayList<>();
        for (User user : userCustomRepository.findAllLatestActive()) {
            users.add(userFactory.createUserDto(
                    (User) unitOfWorkService.aggregateLoadAndRegisterRead(user.getAggregateId(), unitOfWork)));
        }
        return users;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        checkUserNameIsUnique(userDto.getUserName());

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);

        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateUser(Integer userAggregateId, UserDto userDto, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserCopy(oldUser);
        newUser.setPassword(userDto.getPassword());
        newUser.setGender(userDto.getGender());
        newUser.setDocumentType(userDto.getDocumentType());
        newUser.setDocumentNumber(userDto.getDocumentNumber());
        newUser.setEmail(userDto.getEmail());

        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteUser(Integer userAggregateId, UnitOfWork unitOfWork) {
        User oldUser = (User) unitOfWorkService.aggregateLoadAndRegisterRead(userAggregateId, unitOfWork);
        User newUser = userFactory.createUserCopy(oldUser);
        newUser.remove();

        unitOfWorkService.registerChanged(newUser, unitOfWork);
    }

    // UNIQUE_USER_NAME is a create-only guard: userName is final, so no update path can break it.
    private void checkUserNameIsUnique(String userName) {
        for (User user : userCustomRepository.findAllLatestActive()) {
            if (user.getUserName().equals(userName)) {
                throw new TrainticketException(DUPLICATE_USER_NAME);
            }
        }
    }
}
