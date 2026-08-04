package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService;

@Service
public class StationEventProcessing {
    @Autowired
    private StationService stationService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public StationEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}