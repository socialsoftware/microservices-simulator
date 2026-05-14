package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.subscribe.QuizSubscribesDeleteCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.subscribe.QuizSubscribesDeleteQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.subscribe.QuizSubscribesUpdateQuestion;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/*
    INTRA-INVARIANTS:
        QUIZ_DATE_ORDERING: creationDate < availableDate < conclusionDate <= resultsDate
        QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE: once quiz is past availableDate, key fields and questions are frozen
    INTER-INVARIANTS:
        QUESTION_EXISTS (subscribes to UpdateQuestionEvent, DeleteQuestionEvent)
        COURSE_EXECUTION_EXISTS (subscribes to DeleteCourseExecutionEvent)
 */
@Entity
public abstract class Quiz extends Aggregate {

    @Column
    private String title;

    @Column
    private LocalDateTime creationDate;

    @Column
    private LocalDateTime availableDate;

    @Column
    private LocalDateTime conclusionDate;

    @Column
    private LocalDateTime resultsDate;

    @Enumerated(EnumType.STRING)
    private QuizType quizType;

    @Column
    private LocalDateTime lastModifiedTime;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quiz")
    private QuizExecution quizExecution;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuizQuestion> questions = new HashSet<>();

    public Quiz() {}

    public Quiz(Integer aggregateId, String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                LocalDateTime resultsDate, QuizType quizType, QuizExecution quizExecution, Set<QuizQuestion> questions) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.title = title;
        this.creationDate = LocalDateTime.now();
        this.lastModifiedTime = this.creationDate;
        setAvailableDate(availableDate);
        setConclusionDate(conclusionDate);
        setResultsDate(resultsDate);
        this.quizType = quizType;
        setQuizExecution(quizExecution);
        setQuestions(questions);
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
        setQuizExecution(new QuizExecution(other.getQuizExecution()));
        for (QuizQuestion q : other.getQuestions()) {
            this.questions.add(new QuizQuestion(q));
        }
    }

    private boolean quizDateOrdering() {
        if (creationDate == null || availableDate == null || conclusionDate == null || resultsDate == null) {
            return true;
        }
        return creationDate.isBefore(availableDate)
                && availableDate.isBefore(conclusionDate)
                && !conclusionDate.isAfter(resultsDate);
    }

    private boolean quizFieldsFinalAfterAvailableDate() {
        if (getPrev() == null || !(getPrev() instanceof Quiz)) {
            return true;
        }
        Quiz prev = (Quiz) getPrev();
        if (prev.getAvailableDate() == null || lastModifiedTime == null) {
            return true;
        }
        if (lastModifiedTime.isAfter(prev.getAvailableDate())) {
            Set<Integer> currentIds = questions.stream()
                    .map(QuizQuestion::getQuestionAggregateId).collect(Collectors.toSet());
            Set<Integer> prevIds = prev.getQuestions().stream()
                    .map(QuizQuestion::getQuestionAggregateId).collect(Collectors.toSet());
            return availableDate.equals(prev.getAvailableDate())
                    && conclusionDate.equals(prev.getConclusionDate())
                    && resultsDate.equals(prev.getResultsDate())
                    && currentIds.equals(prevIds);
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (!quizDateOrdering()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.QUIZ_DATE_ORDERING);
        }
        if (!quizFieldsFinalAfterAvailableDate()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> subscriptions = new HashSet<>();
        if (getState() == AggregateState.ACTIVE) {
            for (QuizQuestion question : questions) {
                subscriptions.add(new QuizSubscribesUpdateQuestion(question));
                subscriptions.add(new QuizSubscribesDeleteQuestion(question));
            }
            subscriptions.add(new QuizSubscribesDeleteCourseExecution(quizExecution));
        }
        return subscriptions;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreationDate() { return creationDate; }

    public LocalDateTime getAvailableDate() { return availableDate; }
    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
        this.lastModifiedTime = LocalDateTime.now();
    }

    public LocalDateTime getConclusionDate() { return conclusionDate; }
    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
        this.lastModifiedTime = LocalDateTime.now();
    }

    public LocalDateTime getResultsDate() { return resultsDate; }
    public void setResultsDate(LocalDateTime resultsDate) {
        this.resultsDate = resultsDate;
        this.lastModifiedTime = LocalDateTime.now();
    }

    public QuizType getQuizType() { return quizType; }
    public void setQuizType(QuizType quizType) { this.quizType = quizType; }

    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }

    public QuizExecution getQuizExecution() { return quizExecution; }
    public void setQuizExecution(QuizExecution quizExecution) {
        this.quizExecution = quizExecution;
        this.quizExecution.setQuiz(this);
    }

    public Set<QuizQuestion> getQuestions() { return questions; }
    public void setQuestions(Set<QuizQuestion> questions) {
        this.questions = questions;
        this.lastModifiedTime = LocalDateTime.now();
    }
    public void addQuestion(QuizQuestion question) {
        this.questions.add(question);
        this.lastModifiedTime = LocalDateTime.now();
    }
    public void removeQuestion(QuizQuestion question) {
        this.questions.remove(question);
        this.lastModifiedTime = LocalDateTime.now();
    }
}
