package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.subscribe.QuizSubscribesDeleteCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.subscribe.QuizSubscribesDeleteQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.subscribe.QuizSubscribesUpdateQuestion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "quizzes")
public abstract class Quiz extends Aggregate {
    private String title;
    private final LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private LocalDateTime lastModifiedTime;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "quiz")
    private QuizExecution execution;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizQuestion> questions = new ArrayList<>();

    public Quiz() {
        this.creationDate = null;
    }

    public Quiz(Integer aggregateId, Integer executionAggregateId, Long executionVersion, String title,
                LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate,
                LocalDateTime resultsDate, QuizType quizType) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.title = title;
        this.creationDate = creationDate;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
        this.quizType = quizType;
        this.lastModifiedTime = creationDate;
        setExecution(new QuizExecution(executionAggregateId, executionVersion));
    }

    public Quiz(Quiz other) {
        super(other);
        this.title = other.getTitle();
        this.creationDate = other.getCreationDate();
        this.availableDate = other.getAvailableDate();
        this.conclusionDate = other.getConclusionDate();
        this.resultsDate = other.getResultsDate();
        this.quizType = other.getQuizType();
        this.lastModifiedTime = other.getLastModifiedTime();
        setExecution(new QuizExecution(other.getExecution()));
        this.questions = other.getQuestions().stream()
                .map(QuizQuestion::new)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyInvariants() {
        quizDateOrdering();
        fieldsFinalAfterAvailableDate();
    }

    private void quizDateOrdering() {
        boolean ordered = this.creationDate.isBefore(this.availableDate)
                && this.availableDate.isBefore(this.conclusionDate)
                && !this.conclusionDate.isAfter(this.resultsDate);
        if (!ordered) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.QUIZ_DATE_ORDERING);
        }
    }

    private void fieldsFinalAfterAvailableDate() {
        Quiz prev = (Quiz) getPrev();
        if (prev == null || !this.lastModifiedTime.isAfter(prev.getAvailableDate())) {
            return;
        }
        boolean unchanged = Objects.equals(this.availableDate, prev.getAvailableDate())
                && Objects.equals(this.conclusionDate, prev.getConclusionDate())
                && Objects.equals(this.resultsDate, prev.getResultsDate())
                && questionAggregateIds().equals(prev.questionAggregateIds());
        if (!unchanged) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE);
        }
    }

    // Membership, not full equality: the frozen field is which questions the quiz asks. The cached
    // title, content and version of a QuizQuestion are refreshed by the UpdateQuestionEvent handler,
    // which must keep working after the quiz becomes available.
    private Set<Integer> questionAggregateIds() {
        return this.questions.stream()
                .map(QuizQuestion::getQuestionAggregateId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (getState() == AggregateState.ACTIVE) {
            eventSubscriptions.add(new QuizSubscribesDeleteCourseExecution(this.execution));
            for (QuizQuestion question : this.questions) {
                eventSubscriptions.add(new QuizSubscribesUpdateQuestion(question));
                eventSubscriptions.add(new QuizSubscribesDeleteQuestion(question));
            }
        }
        return eventSubscriptions;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        stampLastModifiedTime();
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
        stampLastModifiedTime();
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
        stampLastModifiedTime();
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(LocalDateTime resultsDate) {
        this.resultsDate = resultsDate;
        stampLastModifiedTime();
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
        stampLastModifiedTime();
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    // QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE compares this stamp against prev.availableDate, so the
    // clock is read here and never inside verifyInvariants().
    private void stampLastModifiedTime() {
        this.lastModifiedTime = DateHandler.now();
    }

    public QuizExecution getExecution() {
        return execution;
    }

    // No setter: the Execution reference is immutable (QUIZ_COURSE_EXECUTION_FINAL). Both
    // constructors install the snapshot through here so the back-reference mappedBy = "quiz"
    // resolves is always wired before the entity is persisted.
    private void setExecution(QuizExecution execution) {
        this.execution = execution;
        execution.setQuiz(this);
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
        stampLastModifiedTime();
    }

    public void addQuestion(QuizQuestion question) {
        this.questions.add(question);
        stampLastModifiedTime();
    }

    public void removeQuestion(Integer questionAggregateId) {
        this.questions.removeIf(question -> questionAggregateId.equals(question.getQuestionAggregateId()));
        stampLastModifiedTime();
    }
}
