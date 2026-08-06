package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.service.UserService;

@Service
public class UserEventProcessing {
    @Autowired
    private UserService userService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public UserEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}