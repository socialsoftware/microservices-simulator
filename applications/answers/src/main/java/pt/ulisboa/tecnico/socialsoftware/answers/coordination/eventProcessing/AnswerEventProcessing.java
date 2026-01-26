package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;

@Service
public class AnswerEventProcessing {
    @Autowired
    private AnswerService answerService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public AnswerEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}