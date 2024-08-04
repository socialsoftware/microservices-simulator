package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.QUESTION_ALREADY_ANSWERED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.MergeableAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe.QuizAnswerSubscribesAnonymizeStudent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe.QuizAnswerSubscribesInvalidateQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe.QuizAnswerSubscribesRemoveCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe.QuizAnswerSubscribesUnerollStudentFromCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe.QuizAnswerSubscribesUpdateStudentName;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

/*
    INTRA-INVARIANTS:
        FINAL_ANSWER_DATE
        FINAL_CREATION_DATE
        FINAL_USER
        FINAL_QUIZ
        FINAL_COURSE_EXECUTION
    INTER-INVARIANTS:
        USER_EXISTS
        QUIZ_EXISTS
        QUESTION_EXISTS
        COURSE_EXECUTION_EXISTS
        QUESTION_ANSWERS_QUESTION_BELONGS_TO_QUIZ
        QUIZ_COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION (verified at the service and the quiz doesnt change execution)

 */
@Entity
public abstract class QuizAnswer extends Aggregate implements MergeableAggregate {
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private boolean completed;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quizAnswer")
    private AnswerCourseExecution answerCourseExecution;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quizAnswer")
    private AnswerStudent student;
    /* it is not final because of the question ids inside*/
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quizAnswer")
    private AnsweredQuiz quiz;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "quizAnswer")
    private List<QuestionAnswer> questionAnswers = new ArrayList<>();

    public QuizAnswer() {
        setAnswerCourseExecution(new AnswerCourseExecution());
        setStudent(new AnswerStudent());
        setQuiz(new AnsweredQuiz());
    }

    public QuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAnswerCourseExecution(answerCourseExecution);
        setStudent(answerStudent);
        setQuiz(answeredQuiz);
    }

    public QuizAnswer(QuizAnswer other) {
        super(other);
        setAnswerCourseExecution(new AnswerCourseExecution(other.getAnswerCourseExecution()));
        setStudent(new AnswerStudent(other.getStudent()));
        setQuiz(new AnsweredQuiz(other.getQuiz()));
        setAnswerDate(other.getAnswerDate());
        setCreationDate(other.getCreationDate());
        setQuestionAnswers(other.getQuestionAnswers().stream().map(QuestionAnswer::new).collect(Collectors.toList()));
    }

    @Override
    public void verifyInvariants() {

    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        //return Set.of(REMOVE_USER, UNENROLL_STUDENT, INVALIDATE_QUIZ/*, REMOVE_QUIZ*/);
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (getState() == ACTIVE) {
            interInvariantCourseExecutionExists(eventSubscriptions);
            interInvariantQuizExists(eventSubscriptions);
            interInvariantStudentExists(eventSubscriptions);
        }
        return eventSubscriptions;
    }

    private void interInvariantCourseExecutionExists(Set<EventSubscription> eventSubscriptions) {
        // TODO: this event is not handled
        eventSubscriptions.add(new QuizAnswerSubscribesRemoveCourseExecution(this.getAnswerCourseExecution()));
    }

    private void interInvariantQuizExists(Set<EventSubscription> eventSubscriptions) {
        // also verifies QUESTION_EXISTS because if the question is DELETED the quiz sends this event
        // TODO: this event is not handled
        eventSubscriptions.add(new QuizAnswerSubscribesInvalidateQuiz(this));
    }

    private void interInvariantStudentExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new QuizAnswerSubscribesUnerollStudentFromCourseExecution(this));
        // TODO: this event is not handled
        eventSubscriptions.add(new QuizAnswerSubscribesAnonymizeStudent(this));
        eventSubscriptions.add(new QuizAnswerSubscribesUpdateStudentName(this));
    }


    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public AnswerCourseExecution getAnswerCourseExecution() {
        return answerCourseExecution;
    }

    public void setAnswerCourseExecution(AnswerCourseExecution answerCourseExecution) {
        this.answerCourseExecution = answerCourseExecution;
        answerCourseExecution.setQuizAnswer(this);
    }

    public AnswerStudent getStudent() {
        return student;
    }

    public void setStudent(AnswerStudent answerStudent) {
        this.student = answerStudent;
        answerStudent.setQuizAnswer(this);
    }

    public AnsweredQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(AnsweredQuiz quiz) {
        this.quiz = quiz;
        quiz.setQuizAnswer(this);
    }

    public List<QuestionAnswer> getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(List<QuestionAnswer> questionAnswers) {
        this.questionAnswers.forEach(questionAnswer -> questionAnswer.setQuizAnswer(null));
        this.questionAnswers = questionAnswers;
        questionAnswers.forEach(questionAnswer -> questionAnswer.setQuizAnswer(this));
    }

    public void addQuestionAnswer(QuestionAnswer questionAnswer) {
        List<Integer> answeredQuestionIds = this.questionAnswers.stream()
                .map(QuestionAnswer::getQuestionAggregateId)
                .collect(Collectors.toList());
        if (answeredQuestionIds.contains(questionAnswer.getQuestionAggregateId())) {
            throw new TutorException(QUESTION_ALREADY_ANSWERED, questionAnswer.getQuestionAggregateId(), this.getQuiz().getQuizAggregateId());
        }
        this.questionAnswers.add(questionAnswer);
        questionAnswer.setQuizAnswer(this);
    }

    public QuestionAnswer findQuestionAnswer(Integer questionAggregateId) {
        for (QuestionAnswer qa : this.questionAnswers) {
            if (qa.getQuestionAggregateId().equals(questionAggregateId)) {
                return qa;
            }
        }
        return null;
    }

    @Override
    public Set<String> getMutableFields() {
        return Set.of("questionAnswers", "answerDate");
    }

    @Override
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        QuizAnswer committedQuizAnswer = (QuizAnswer) committedVersion;
        mergeCourseExecution(this, committedQuizAnswer);
        mergeUser(this, committedQuizAnswer);
        mergeQuiz(this, committedQuizAnswer);
        mergeAnswerDate(toCommitVersionChangedFields, this, committedQuizAnswer);
        mergeQuestionAnswers((QuizAnswer)getPrev(), this, committedQuizAnswer, this);
        return this;
    }

    private void mergeCourseExecution(QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        // The course execution version determines which user is more recent because the user is an execution student
        if (toCommitQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion() >= committedQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion()) {
            toCommitQuizAnswer.getAnswerCourseExecution().setCourseExecutionVersion(toCommitQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion());
        } else {
            toCommitQuizAnswer.getAnswerCourseExecution().setCourseExecutionVersion(committedQuizAnswer.getAnswerCourseExecution().getCourseExecutionVersion());
        }
    }

    private void mergeUser(QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        // The course execution version determines which user is more recent because the user is an execution student
        /*if(toCommitAnswer.getCourseExecution().getVersion() >= committedAnswer.getCourseExecution().getVersion()) {
            toCommitAnswer.getUser(getUser());
        } else {
            toCommitAnswer.getUser().(committedAnswer.getUser());
        }*/
    }

    private void mergeQuiz(QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        if(getQuiz().getQuizVersion() >= committedQuizAnswer.getQuiz().getQuizVersion()) {
            toCommitQuizAnswer.setQuiz(new AnsweredQuiz(toCommitQuizAnswer.getQuiz()));
        } else {
            toCommitQuizAnswer.setQuiz(new AnsweredQuiz(committedQuizAnswer.getQuiz()));

        }
    }

    private void mergeAnswerDate(Set<String> toCommitVersionChangedFields, QuizAnswer toCommitQuizAnswer, QuizAnswer committedQuizAnswer) {
        if(toCommitVersionChangedFields.contains("answerDate")) {
            toCommitQuizAnswer.setAnswerDate(getAnswerDate());
        } else {
            toCommitQuizAnswer.setAnswerDate(committedQuizAnswer.getAnswerDate());
        }
    }

    private static void mergeQuestionAnswers(QuizAnswer prev, QuizAnswer v1, QuizAnswer v2, QuizAnswer mergedQuizAnswer) {
        /* Here we "calculate" the result of the incremental fields. This fields will always be the same regardless
         * of the base we choose. */

        Set<QuestionAnswer> prevQuestionAnswersPre = new HashSet<>(prev.getQuestionAnswers());
        Set<QuestionAnswer> v1QuestionAnswersPre = new HashSet<>(v1.getQuestionAnswers());
        Set<QuestionAnswer> v2QuestionAnswersPre = new HashSet<>(v2.getQuestionAnswers());

        Set<QuestionAnswer> prevQuestionAnswers = new HashSet<>(prevQuestionAnswersPre);
        Set<QuestionAnswer> v1QuestionAnswers = new HashSet<>(v1QuestionAnswersPre);
        Set<QuestionAnswer> v2QuestionAnswers = new HashSet<>(v2QuestionAnswersPre);


        Set<QuestionAnswer> addedQuestionAnswers =  SetUtils.union(
                SetUtils.difference(v1QuestionAnswers, prevQuestionAnswers),
                SetUtils.difference(v2QuestionAnswers, prevQuestionAnswers)
        );

        Set<QuestionAnswer> removedQuestionAnswers = SetUtils.union(
                SetUtils.difference(prevQuestionAnswers, v1QuestionAnswers),
                SetUtils.difference(prevQuestionAnswers, v2QuestionAnswers)
        );

        Set<QuestionAnswer> mergedQuestionAnswers = SetUtils.union(SetUtils.difference(prevQuestionAnswers, removedQuestionAnswers), addedQuestionAnswers);
        mergedQuizAnswer.setQuestionAnswers(new ArrayList<>(mergedQuestionAnswers));

    }

}
