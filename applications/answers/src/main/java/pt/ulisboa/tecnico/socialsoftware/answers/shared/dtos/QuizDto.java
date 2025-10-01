package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class QuizDto implements Serializable {
    
    private Integer id;
    private String title;
    private String description;
    private String quizType;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private Integer numberOfQuestions;
    
    public QuizDto() {
        // Default constructor
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
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

    public String getQuizType() {
        return quizType;
    }
    
    public void setQuizType(String quizType) {
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
}