package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.DeleteQuestionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.DeleteUserEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.DisenrollStudentFromCourseExecutionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.UpdateStudentNameEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;

@Component
public class QuizAnswerEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;
    @Autowired
    private QuizAnswerEventProcessing quizAnswerEventProcessing;
    /*
        QUIZ_COURSE_EXECUTION_SAME_AS_QUESTION_COURSE
     */

    /** NO EVENT FOR THESE neither question course or quiz course execution changed**/

    /*
        USER_EXISTS
	*/

    /*
        USER_EXISTS
	*/

    // TODO: no event associated
    @Scheduled(fixedDelay = 1000)
    public void handleRemoveUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class,
                new DeleteUserEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
        QUIZ_EXISTS
            Wait fo the quiz to emit events
     */

    /*
        QUESTION_EXISTS
            Should we process this??? The answer only has the ids
     */

    // TODO: no event associated
    @Scheduled(fixedDelay = 1000)
    public void handleRemoveQuestionEvent() {
        eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class,
                new DeleteQuestionEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
    QUIZ_COURSE_EXECUTION_SAME_AS_USER
     */

    @Scheduled(fixedDelay = 1000)
    public void handleUnenrollStudentEvent() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class,
                new DisenrollStudentFromCourseExecutionEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateExecutionStudentNameEvent() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class,
                new UpdateStudentNameEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

}

