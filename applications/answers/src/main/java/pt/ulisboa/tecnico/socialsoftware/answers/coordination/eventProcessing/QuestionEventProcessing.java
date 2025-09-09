package com.generated.microservices.answers.coordination.eventProcessing;

import static com.generated.microservices.ms.TransactionalModel.SAGAS;
import static com.generated.microservices.answers.microservices.exception.AnswersErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import com.generated.microservices.ms.TransactionalModel;
import com.generated.microservices.ms.coordination.unitOfWork.UnitOfWork;
import com.generated.microservices.ms.coordination.unitOfWork.UnitOfWorkService;
import com.generated.microservices.answers.microservices.exception.AnswersException;
import com.generated.microservices.answers.microservices.question.service.QuestionService;

@Service
public class QuestionEventProcessing {
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private UnitOfWorkService unitOfWorkService;
    
    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = Arrays.asList(activeProfiles).contains(SAGAS.getValue()) ? SAGAS : null;
        if (workflowType == null) {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void processQuestionEvent(String eventType, Integer aggregateId, Integer aggregateVersion) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        unitOfWorkService.commit(unitOfWork);
    }
}