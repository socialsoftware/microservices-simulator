package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.handlers.*;

@Component
public class TournamentEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TournamentEventProcessing tournamentEventProcessing;

    /* fixed delay guarantees this task only runs 10 seconds after the previous finished. With fixed delay concurrent executions are not possible.*/
    /*
    CREATOR_EXISTS
		this.creator.state != INACTIVE => EXISTS User(this.creator.id) && this.creator.username == User(this.creator.id).username && this.creator.name == User(this.creator.id).name
    PARTICIPANT_EXISTS
		Rule:
			forall p : this.tournamentParticipants | p.state != INACTIVE => EXISTS User(p.id) && p.username == User(p.id).username && p.name == User(p.id).name
		Events Subscribed:
			anonymizeUser(user: User) {
				p in this.participants | p.id == user.id
					p.username = user.username
					p.name = user.name
					p.state = INACTIVE
	*/

    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class,
                new AnonymizeStudentEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    /*
    COURSE_EXECUTION_EXISTS
		this.tournamentCourseExecution.state != INACTIVE => EXISTS CourseExecution(this.tournamentCourseExecution.id) // change do DELETED???
		&& this.courseExecution.courseId == CourseExecution(this.courseExecution.id).Course.id && this.courseExecution.status == CourseExecution(this.courseExecution.id).status
		&& this.courseExecution.acronym == CourseExecution(this.courseExecution.id).acronym
     */

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class,
                new DeleteCourseExecutionEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    /*
        TOPIC_EXISTS
            t: this.tournamentTopics | t.state != INACTIVE => EXISTS Topic(t.id) && t.name == Topic(t.id).name
    */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateTopicEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class,
                new UpdateTopicEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    /*
        TOPIC_EXISTS
            t: this.tournamentTopics | t.state != INACTIVE => EXISTS Topic(t.id) && t.name == Topic(t.id).name
    */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteTopicEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class,
                new DeleteTopicEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    /*
        QUIZ_ANSWER_EXISTS
            p: this.participants | (!p.answer.isEmpty && p.answer.state != INACTIVE) => EXISTS QuizAnswer(p.answer.id)
     */
    @Scheduled(fixedDelay = 1000)
    public void handleAnswerQuestionEvent() {
        eventApplicationService.handleSubscribedEvent(QuizAnswerQuestionAnswerEvent.class,
                new QuizAnswerQuestionAnswerEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    /*
        CREATOR_COURSE_EXECUTION
        PARTICIPANT_COURSE_EXECUTION
     */
    
    @Scheduled(fixedDelay = 1000)
    public void handleUnenrollStudentFromCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class,
                new DisenrollStudentFromCourseExecutionEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    /*
        QUIZ_EXISTS
            this.tournamentQuiz.state != INACTIVE => EXISTS Quiz(this.tournamentQuiz.id)
    */
    @Scheduled(fixedDelay = 1000)
    public void handleInvalidateQuizEvent() {
        eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class,
                new InvalidateQuizEventHandler(tournamentRepository, tournamentEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvent() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class,
                new UpdateStudentNameEventHandler(tournamentRepository, tournamentEventProcessing));
    }

}