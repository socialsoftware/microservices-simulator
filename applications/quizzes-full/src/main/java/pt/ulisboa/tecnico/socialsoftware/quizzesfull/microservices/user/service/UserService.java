package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserFactory;

@Service
public class UserService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final UserCustomRepository userCustomRepository;

    @Autowired
    private UserFactory userFactory;

    public UserService(UnitOfWorkService unitOfWorkService, UserCustomRepository userCustomRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.userCustomRepository = userCustomRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto getUserById(Integer aggregateId, UnitOfWork unitOfWork) {
        return userFactory.createUserDto(
                (User) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserDto createUser(UserDto userDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        User user = userFactory.createUser(aggregateId, userDto);
        unitOfWorkService.registerChanged(user, unitOfWork);
        return userFactory.createUserDto(user);
    }
}
