package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;

@Service
public class TaskEventProcessing {
    @Autowired
    private TaskService taskService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TaskEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}