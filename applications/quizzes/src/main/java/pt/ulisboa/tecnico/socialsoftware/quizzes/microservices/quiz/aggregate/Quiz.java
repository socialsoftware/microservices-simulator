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
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.CANNOT_UPDATE_QUIZ;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.INVARIANT_BREAK;

/*
    INTRA-INVARIANTS
        DATE_ORDERING
        QUESTIONS_FINAL_AFTER_AVAILABLE_DATE
        COURSE_EXECUTION_FINAL
        CREATION_DATE_FINAL
        AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE
        CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE
        RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE
    INTER-INVARIANTS
        QUESTION_EXISTS
        COURSE_EXECUTION_EXISTS
        QUESTION_COURSE_EXECUTION_SAME_AS_COURSE_EXECUTION
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

        setQuizQuestions(other.getQuizQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet()));
        setTitle(other.getTitle());
        this.creationDate = other.getCreationDate();
        setAvailableDate(other.getAvailableDate());
        setConclusionDate(other.getConclusionDate());
        setResultsDate(other.getResultsDate());
        setQuizType(other.getQuizType());
        setQuizQuestions(other.getQuizQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet()));
    }

    public boolean invariantDateOrdering() {
        return getCreationDate().isBefore(getConclusionDate()) &&
                getAvailableDate().isBefore(getConclusionDate()) &&
                (getConclusionDate().isEqual(getResultsDate()) || getConclusionDate().isBefore(getResultsDate()));
    }

    @Override
    public void verifyInvariants() {
        if (!(invariantDateOrdering())) {
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
            AVAILABLE_DATE_FINAL_AFTER_AVAILABLE_DATE
         */
        Quiz prev = (Quiz) getPrev();
        if (prev != null && prev.getAvailableDate() != null && DateHandler.now().isAfter(prev.getAvailableDate())) {
            throw new QuizzesException(CANNOT_UPDATE_QUIZ, getAggregateId());
        }
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        /*
            CONCLUSION_DATE_FINAL_AFTER_AVAILABLE_DATE
         */
        Quiz prev = (Quiz) getPrev();
        if (prev != null && (DateHandler.now()).isAfter(prev.getAvailableDate())) {
            throw new QuizzesException(CANNOT_UPDATE_QUIZ, getAggregateId());
        }
        this.conclusionDate = conclusionDate;
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(LocalDateTime resultsDate) {
        /*
            RESULTS_DATE_FINAL_AFTER_AVAILABLE_DATE
         */
        Quiz prev = (Quiz) getPrev();
        if(prev != null && (DateHandler.now()).isAfter(prev.getAvailableDate())) {
            throw new QuizzesException(CANNOT_UPDATE_QUIZ, getAggregateId());
        }
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
            QUESTIONS_FINAL_AFTER_AVAILABLE_DATE
         */
        Quiz prev = (Quiz) getPrev();
        if (prev != null && DateHandler.now().isAfter(prev.getAvailableDate())) {
            throw new QuizzesException(CANNOT_UPDATE_QUIZ, getAggregateId());
        }
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
}
