package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class QuestionDto implements Serializable {
    
    private Integer id;
    private String title;
    private String content;
    private Integer numberOfOptions;
    private Integer correctOption;
    private Integer order;
    
    public QuestionDto() {
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

    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }

    public Integer getNumberOfOptions() {
        return numberOfOptions;
    }
    
    public void setNumberOfOptions(Integer numberOfOptions) {
        this.numberOfOptions = numberOfOptions;
    }

    public Integer getCorrectOption() {
        return correctOption;
    }
    
    public void setCorrectOption(Integer correctOption) {
        this.correctOption = correctOption;
    }

    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
}