package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;

import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/*
    INTRA-INVARIANTS:
        QUESTION_ALREADY_ANSWERED: all questionIds in questionAnswers are distinct
    INTER-INVARIANTS:
        USER_EXISTS (subscribes to DeleteUserEvent, UpdateStudentNameEvent, AnonymizeStudentEvent, DisenrollStudentFromCourseExecutionEvent)
        QUIZ_EXISTS (subscribes to InvalidateQuizEvent)
        COURSE_EXECUTION_EXISTS (subscribes to DeleteCourseExecutionEvent)
        (QuestionAnswer UpdateQuestionEvent is also subscribed — handled in session d)
 */
@Entity
@Table(name = "quiz_answer")
public abstract class QuizAnswer extends Aggregate {

    @Column
    private final LocalDateTime creationDate;

    @Column
    private final LocalDateTime answerDate;

    @Column
    private Boolean completed;

    @Column
    private final Integer quizAggregateId;

    @Column
    private Long quizVersion;

    @Column
    private final Integer userAggregateId;

    @Column
    private Long userVersion;

    @Column
    private String userName;

    @Column
    private String userUsername;

    @Column
    private final Integer executionAggregateId;

    @Column
    private Long executionVersion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionAnswer> questionAnswers = new HashSet<>();

    public QuizAnswer() {
        this.creationDate = null;
        this.answerDate = null;
        this.quizAggregateId = null;
        this.userAggregateId = null;
        this.executionAggregateId = null;
    }

    public QuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion,
                      Integer userAggregateId, Long userVersion, String userName, String userUsername,
                      Integer executionAggregateId, Long executionVersion) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.creationDate = LocalDateTime.now();
        this.answerDate = LocalDateTime.now();
        this.completed = false;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.userAggregateId = userAggregateId;
        this.userVersion = userVersion;
        this.userName = userName;
        this.userUsername = userUsername;
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
    }

    public QuizAnswer(QuizAnswer other) {
        super(other);
        this.creationDate = other.getCreationDate();
        this.answerDate = other.getAnswerDate();
        this.completed = other.getCompleted();
        this.quizAggregateId = other.getQuizAggregateId();
        this.quizVersion = other.getQuizVersion();
        this.userAggregateId = other.getUserAggregateId();
        this.userVersion = other.getUserVersion();
        this.userName = other.getUserName();
        this.userUsername = other.getUserUsername();
        this.executionAggregateId = other.getExecutionAggregateId();
        this.executionVersion = other.getExecutionVersion();
        for (QuestionAnswer qa : other.getQuestionAnswers()) {
            this.questionAnswers.add(new QuestionAnswer(qa));
        }
    }

    private boolean questionAlreadyAnswered() {
        Set<Integer> ids = questionAnswers.stream()
                .map(QuestionAnswer::getQuestionAggregateId)
                .collect(Collectors.toSet());
        return ids.size() == questionAnswers.size();
    }

    @Override
    public void verifyInvariants() {
        if (!questionAlreadyAnswered()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.QUESTION_ALREADY_ANSWERED);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> subscriptions = new HashSet<>();
        subscriptions.add(new QuizAnswerSubscribesDeleteUser(this));
        subscriptions.add(new QuizAnswerSubscribesUpdateStudentName(this));
        subscriptions.add(new QuizAnswerSubscribesAnonymizeStudent(this));
        subscriptions.add(new QuizAnswerSubscribesDeleteCourseExecution(this));
        subscriptions.add(new QuizAnswerSubscribesDisenrollStudentFromCourseExecution(this));
        subscriptions.add(new QuizAnswerSubscribesInvalidateQuiz(this));
        for (QuestionAnswer qa : questionAnswers) {
            subscriptions.add(new QuizAnswerSubscribesUpdateQuestion(qa));
        }
        return subscriptions;
    }

    public LocalDateTime getCreationDate() { return creationDate; }

    public LocalDateTime getAnswerDate() { return answerDate; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public Integer getQuizAggregateId() { return quizAggregateId; }

    public Long getQuizVersion() { return quizVersion; }
    public void setQuizVersion(Long quizVersion) { this.quizVersion = quizVersion; }

    public Integer getUserAggregateId() { return userAggregateId; }

    public Long getUserVersion() { return userVersion; }
    public void setUserVersion(Long userVersion) { this.userVersion = userVersion; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public Integer getExecutionAggregateId() { return executionAggregateId; }

    public Long getExecutionVersion() { return executionVersion; }
    public void setExecutionVersion(Long executionVersion) { this.executionVersion = executionVersion; }

    public Set<QuestionAnswer> getQuestionAnswers() { return questionAnswers; }
    public void setQuestionAnswers(Set<QuestionAnswer> questionAnswers) { this.questionAnswers = questionAnswers; }
    public void addQuestionAnswer(QuestionAnswer questionAnswer) { this.questionAnswers.add(questionAnswer); }
}
