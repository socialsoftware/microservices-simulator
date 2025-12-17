package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizUpdatedEvent extends Event {
    private String title;
    private LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;

    public QuizUpdatedEvent() {
    }

    public QuizUpdatedEvent(Integer aggregateId, String title, LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate, LocalDateTime resultsDate) {
        super(aggregateId);
        setTitle(title);
        setCreationDate(creationDate);
        setAvailableDate(availableDate);
        setConclusionDate(conclusionDate);
        setResultsDate(resultsDate);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

}