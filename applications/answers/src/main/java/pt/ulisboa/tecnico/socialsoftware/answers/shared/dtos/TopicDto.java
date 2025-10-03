package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TopicDto implements Serializable {
    
    private Integer id;
    private String name;
    private LocalDateTime creationDate;
    
    public TopicDto() {
    }
    
    public TopicDto(Integer id, String name, LocalDateTime creationDate) {
        setId(id);
        setName(name);
        setCreationDate(creationDate);
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}