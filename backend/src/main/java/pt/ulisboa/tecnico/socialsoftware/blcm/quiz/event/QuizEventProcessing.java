package pt.ulisboa.tecnico.socialsoftware.blcm.quiz.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.blcm.execution.event.publish.RemoveCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.blcm.question.event.publish.RemoveQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.blcm.question.event.publish.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.blcm.quiz.service.QuizService;

@Service
public class QuizEventProcessing {
    @Autowired
    private QuizService quizService;
    @Autowired
    private UnitOfWorkService unitOfWorkService;

    public void processRemoveCourseExecutionEvent(Integer aggregateId, RemoveCourseExecutionEvent removeCourseExecutionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        quizService.removeCourseExecution(aggregateId, removeCourseExecutionEvent.getPublisherAggregateId(), removeCourseExecutionEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent updateQuestionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        quizService.updateQuestion(aggregateId, updateQuestionEvent.getPublisherAggregateId(), updateQuestionEvent.getTitle(), updateQuestionEvent.getContent(), updateQuestionEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processRemoveQuizQuestionEvent(Integer aggregateId, RemoveQuestionEvent removeQuestionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        quizService.removeQuizQuestion(aggregateId, removeQuestionEvent.getPublisherAggregateId(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
