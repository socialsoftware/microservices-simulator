package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.subscribe.QuizSubscribesDeleteCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.subscribe.QuizSubscribesDeleteQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.subscribe.QuizSubscribesUpdateQuestion;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.INVARIANT_BREAK;

/*
    INTRA-INVARIANTS
        QUESTIONS_FINAL_AFTER_AVAILABLE_DATE
        COURSE_EXECUTION_FINAL
        CREATION_DATE_FINAL
        AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE
        CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE
        RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE
    INTER-INVARIANTS
        QUESTION_EXISTS
        COURSE_EXECUTION_EXISTS
        UNIQUE_QUIZ_ANSWER_PER_STUDENT (Layer 3 guard in QuizAnswerService.startQuiz)
 */
@Entity
public abstract class Quiz extends Aggregate {
    /*
        CREATION_DATE_FINAL
     */
    private final LocalDateTime creationDate;
    protected LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private String title = "Title";
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "quiz")
    private Set<QuizQuestion> quizQuestions = new HashSet<>();
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    /*
        COURSE_EXECUTION_FINAL
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quiz")
    private QuizCourseExecution quizCourseExecution;
    /*
     * Timestamp captured at mutation time so that QUESTIONS_FINAL_AFTER_AVAILABLE_DATE,
     * AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE, CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE,
     * and RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE can be verified post-hoc inside
     * verifyInvariants() without calling DateHandler.now() there.
     */
    private LocalDateTime lastModifiedTime;

    public Quiz() {
        this.quizCourseExecution = null;
        this.creationDate = null;
    }

    public Quiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setQuizCourseExecution(quizCourseExecution);
        setQuizQuestions(quizQuestions);
        setTitle(quizDto.getTitle());
        this.creationDate = DateHandler.now();
        setAvailableDate(DateHandler.toLocalDateTime(quizDto.getAvailableDate()));
        setConclusionDate(DateHandler.toLocalDateTime(quizDto.getConclusionDate()));
        setResultsDate(DateHandler.toLocalDateTime(quizDto.getResultsDate()));
        setQuizType(quizType);
    }

    public Quiz(Quiz other) {
        super(other);
        setQuizCourseExecution(new QuizCourseExecution(other.getQuizCourseExecution()));
        // Copy plain fields directly (bypasses setters so lastModifiedTime is not stamped on copy)
        this.quizQuestions = other.getQuizQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet());
        this.quizQuestions.forEach(quizQuestion -> quizQuestion.setQuiz(this));
        setTitle(other.getTitle());
        this.creationDate = other.getCreationDate();
        this.availableDate = other.getAvailableDate();
        this.conclusionDate = other.getConclusionDate();
        this.resultsDate = other.getResultsDate();
        setQuizType(other.getQuizType());
        this.lastModifiedTime = other.getLastModifiedTime();
    }

    public boolean invariantDateOrdering() {
        return getCreationDate().isBefore(getConclusionDate()) &&
                getAvailableDate().isBefore(getConclusionDate()) &&
                (getConclusionDate().isEqual(getResultsDate()) || getConclusionDate().isBefore(getResultsDate()));
    }

    /*
     * QUESTIONS_FINAL_AFTER_AVAILABLE_DATE / AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE
     * CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE / RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE
     * lastModifiedTime > prev.availableDate => final questions, availableDate, conclusionDate, resultsDate
     */
    private boolean invariantFieldsFinalAfterAvailableDate() {
        Quiz prev = (Quiz) getPrev();
        if (prev != null && prev.getAvailableDate() != null && this.lastModifiedTime != null
                && this.lastModifiedTime.isAfter(prev.getAvailableDate())) {
            return Objects.equals(this.availableDate, prev.getAvailableDate())
                    && Objects.equals(this.conclusionDate, prev.getConclusionDate())
                    && Objects.equals(this.resultsDate, prev.getResultsDate())
                    && this.quizQuestions.stream().map(QuizQuestion::getQuestionAggregateId)
                            .collect(Collectors.toSet())
                            .equals(prev.getQuizQuestions().stream().map(QuizQuestion::getQuestionAggregateId)
                                    .collect(Collectors.toSet()));
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (!(invariantDateOrdering() && invariantFieldsFinalAfterAvailableDate())) {
            throw new QuizzesException(INVARIANT_BREAK, getAggregateId());
        }
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (getState() == ACTIVE) {
            interInvariantCourseExecutionExists(eventSubscriptions);
            interInvariantQuestionsExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }

    private void interInvariantCourseExecutionExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new QuizSubscribesDeleteCourseExecution(this.getQuizCourseExecution()));
    }

    private void interInvariantQuestionsExist(Set<EventSubscription> eventSubscriptions) {
        for (QuizQuestion quizQuestion : this.quizQuestions) {
            eventSubscriptions.add(new QuizSubscribesUpdateQuestion(quizQuestion));
            eventSubscriptions.add(new QuizSubscribesDeleteQuestion(quizQuestion));
        }
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        /*
         * AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE — enforced by verifyInvariants() using lastModifiedTime
         */
        setLastModifiedTime(DateHandler.now());
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        /*
         * CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE — enforced by verifyInvariants() using lastModifiedTime
         */
        setLastModifiedTime(DateHandler.now());
        this.conclusionDate = conclusionDate;
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(LocalDateTime resultsDate) {
        /*
         * RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE — enforced by verifyInvariants() using lastModifiedTime
         */
        setLastModifiedTime(DateHandler.now());
        this.resultsDate = resultsDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<QuizQuestion> getQuizQuestions() {
        return quizQuestions;
    }


    public void setQuizQuestions(Set<QuizQuestion> quizQuestions) {
        /*
         * QUESTIONS_FINAL_AFTER_AVAILABLE_DATE — enforced by verifyInvariants() using lastModifiedTime
         */
        setLastModifiedTime(DateHandler.now());
        this.quizQuestions = quizQuestions;
        this.quizQuestions.forEach(quizQuestion -> quizQuestion.setQuiz(this));
    }

    public QuizCourseExecution getQuizCourseExecution() {
        return quizCourseExecution;
    }

    public void setQuizCourseExecution(QuizCourseExecution quizCourseExecution) {
        this.quizCourseExecution = quizCourseExecution;
        this.quizCourseExecution.setQuiz(this);
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }

    public void update(QuizDto quizDto) {
        setTitle(quizDto.getTitle());
        setAvailableDate(DateHandler.toLocalDateTime(quizDto.getAvailableDate()));
        setConclusionDate(DateHandler.toLocalDateTime(quizDto.getConclusionDate()));
        setResultsDate(DateHandler.toLocalDateTime(quizDto.getResultsDate()));
    }

    public QuizQuestion findQuestion(Integer questionAggregateId) {
        for (QuizQuestion qq : quizQuestions) {
            if (qq.getQuestionAggregateId().equals(questionAggregateId)) {
                return qq;
            }
        }
        return null;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
