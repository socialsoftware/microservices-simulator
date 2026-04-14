package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuestionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class QuizEventProcessing {
    @Autowired
    private QuizService quizService;

    @Autowired
    private QuizFactory quizFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public QuizEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processExecutionUpdatedEvent(Integer aggregateId, ExecutionUpdatedEvent executionUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        quizService.handleExecutionUpdatedEvent(aggregateId, executionUpdatedEvent.getPublisherAggregateId(), executionUpdatedEvent.getPublisherAggregateVersion(), executionUpdatedEvent.getAcronym(), executionUpdatedEvent.getAcademicTerm(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicUpdatedEvent(Integer aggregateId, TopicUpdatedEvent topicUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        quizService.handleTopicUpdatedEvent(aggregateId, topicUpdatedEvent.getPublisherAggregateId(), topicUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicDeletedEvent(Integer aggregateId, TopicDeletedEvent topicDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        quizService.handleTopicDeletedEvent(aggregateId, topicDeletedEvent.getPublisherAggregateId(), topicDeletedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processExecutionDeletedEvent(Integer aggregateId, ExecutionDeletedEvent executionDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
        newQuiz.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processQuestionDeletedEvent(Integer aggregateId, QuestionDeletedEvent questionDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
        newQuiz.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}