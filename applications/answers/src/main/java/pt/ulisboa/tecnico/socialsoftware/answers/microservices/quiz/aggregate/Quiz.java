package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

@Entity
public abstract class Quiz extends Aggregate {
    @Id
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private Integer numberOfQuestions;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "quiz")
    private QuizExecution execution;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "quiz")
    private Set<QuizQuestion> questions = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "quiz")
    private Set<QuizOption> options = new HashSet<>(); 

    public Quiz() {
    }

    public Quiz(Integer aggregateId, QuizDto quizDto, QuizExecution execution) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(quizDto.getTitle());
        setDescription(quizDto.getDescription());
        setQuizType(QuizType.valueOf(quizDto.getQuizType()));
        setAvailableDate(quizDto.getAvailableDate());
        setConclusionDate(quizDto.getConclusionDate());
        setNumberOfQuestions(quizDto.getNumberOfQuestions());
        setExecution(execution);
    }

    public Quiz(Quiz other) {
        super(other);
        setTitle(other.getTitle());
        setDescription(other.getDescription());
        setQuizType(other.getQuizType());
        setAvailableDate(other.getAvailableDate());
        setConclusionDate(other.getConclusionDate());
        setNumberOfQuestions(other.getNumberOfQuestions());
        setExecution(new QuizExecution(other.getExecution()));
        setQuestions(other.getQuestions().stream().map(QuizQuestion::new).collect(Collectors.toSet()));
        setOptions(other.getOptions().stream().map(QuizOption::new).collect(Collectors.toSet()));
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
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

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
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
            this.questions.forEach(quizquestion -> quizquestion.setQuiz(this));
        }
    }

    public Set<QuizOption> getOptions() {
        return options;
    }

    public void setOptions(Set<QuizOption> options) {
        this.options = options;
        if (this.options != null) {
            this.options.forEach(quizoption -> quizoption.setQuiz(this));
        }
    }



}