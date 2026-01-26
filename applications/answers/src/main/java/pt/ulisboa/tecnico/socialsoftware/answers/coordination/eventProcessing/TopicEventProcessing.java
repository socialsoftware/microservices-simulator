package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;

@Service
public class TopicEventProcessing {
    @Autowired
    private TopicService topicService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TopicEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}