package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

@Entity
public class QuizUpdatedEvent extends Event {
    private String title;
    private QuizType quizType;
    private LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private QuizExecution execution;
    private Set<QuizQuestion> questions;

    public QuizUpdatedEvent() {
    }

    public QuizUpdatedEvent(Integer aggregateId, String title, QuizType quizType, LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate, LocalDateTime resultsDate, QuizExecution execution, Set<QuizQuestion> questions) {
        super(aggregateId);
        setTitle(title);
        setQuizType(quizType);
        setCreationDate(creationDate);
        setAvailableDate(availableDate);
        setConclusionDate(conclusionDate);
        setResultsDate(resultsDate);
        setExecution(execution);
        setQuestions(questions);
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
    }

    public Set<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<QuizQuestion> questions) {
        this.questions = questions;
    }

}