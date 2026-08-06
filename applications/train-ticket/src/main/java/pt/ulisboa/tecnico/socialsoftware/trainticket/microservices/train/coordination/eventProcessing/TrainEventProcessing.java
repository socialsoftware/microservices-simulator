package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.service.TrainService;

@Service
public class TrainEventProcessing {
    @Autowired
    private TrainService trainService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TrainEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}