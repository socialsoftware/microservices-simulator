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

import java.util.ArrayList;
import java.util.List;

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
}
