package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

@Entity
public abstract class Quiz extends Aggregate {
    private String title;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quiz")
    private QuizExecution execution;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "quiz")
    private Set<QuizQuestion> questions = new HashSet<>();

    public Quiz() {

    }

    public Quiz(Integer aggregateId, QuizExecution execution, QuizDto quizDto, Set<QuizQuestion> questions) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(quizDto.getTitle());
        setQuizType(QuizType.valueOf(quizDto.getQuizType()));
        setCreationDate(quizDto.getCreationDate());
        setAvailableDate(quizDto.getAvailableDate());
        setConclusionDate(quizDto.getConclusionDate());
        setResultsDate(quizDto.getResultsDate());
        setExecution(execution);
        setQuestions(questions);
    }

    public Quiz(Quiz other) {
        super(other);
        setTitle(other.getTitle());
        setQuizType(other.getQuizType());
        setCreationDate(other.getCreationDate());
        setAvailableDate(other.getAvailableDate());
        setConclusionDate(other.getConclusionDate());
        setResultsDate(other.getResultsDate());
        setExecution(new QuizExecution(other.getExecution()));
        setQuestions(other.getQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet()));
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(LocalDateTime resultsDate) {
        this.resultsDate = resultsDate;
    }

    public QuizExecution getExecution() {
        return execution;
    }

    public void setExecution(QuizExecution execution) {
        this.execution = execution;
        if (this.execution != null) {
            this.execution.setQuiz(this);
        }
    }

    public Set<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<QuizQuestion> questions) {
        this.questions = questions;
        if (this.questions != null) {
            this.questions.forEach(item -> item.setQuiz(this));
        }
    }

    public void addQuizQuestion(QuizQuestion quizQuestion) {
        if (this.questions == null) {
            this.questions = new HashSet<>();
        }
        this.questions.add(quizQuestion);
        if (quizQuestion != null) {
            quizQuestion.setQuiz(this);
        }
    }

    public void removeQuizQuestion(Integer id) {
        if (this.questions != null) {
            this.questions.removeIf(item -> 
                item.getQuestionAggregateId() != null && item.getQuestionAggregateId().equals(id));
        }
    }

    public boolean containsQuizQuestion(Integer id) {
        if (this.questions == null) {
            return false;
        }
        return this.questions.stream().anyMatch(item -> 
            item.getQuestionAggregateId() != null && item.getQuestionAggregateId().equals(id));
    }

    public QuizQuestion findQuizQuestionById(Integer id) {
        if (this.questions == null) {
            return null;
        }
        return this.questions.stream()
            .filter(item -> item.getQuestionAggregateId() != null && item.getQuestionAggregateId().equals(id))
            .findFirst()
            .orElse(null);
    }


    @Override
    public void verifyInvariants() {
        // No invariants defined
    }

}