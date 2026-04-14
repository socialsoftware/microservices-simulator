package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class QuestionEventProcessing {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionFactory questionFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public QuestionEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processTopicUpdatedEvent(Integer aggregateId, TopicUpdatedEvent topicUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        questionService.handleTopicUpdatedEvent(aggregateId, topicUpdatedEvent.getPublisherAggregateId(), topicUpdatedEvent.getPublisherAggregateVersion(), topicUpdatedEvent.getName(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processCourseDeletedEvent(Integer aggregateId, CourseDeletedEvent courseDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicDeletedEvent(Integer aggregateId, TopicDeletedEvent topicDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}