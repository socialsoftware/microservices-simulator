package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService;

@Service
public class RoomEventProcessing {
    @Autowired
    private RoomService roomService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public RoomEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}