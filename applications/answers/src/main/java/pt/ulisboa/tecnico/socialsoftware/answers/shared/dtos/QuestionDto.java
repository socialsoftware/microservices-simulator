package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private Integer sequence;
    private Integer timeTaken;
    private Integer optionKey;
    
    public QuestionDto() {
    }
    
    public QuestionDto(Question question) {
        setAggregateId(question.getAggregateId());
        setVersion(question.getVersion());
        setState(question.getState());
        setTitle(question.getTitle());
        setContent(question.getContent());
        setCreationDate(question.getCreationDate());
        setSequence(question.getQuestionCourse().getSequence());
        setTimeTaken(question.getQuestionCourse().getTimeTaken());
        setOptionKey(question.getQuestionCourse().getOptionKey());
    }
    
    public Integer getAggregateId() {
        return aggregateId;
    }
    
    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }
    
    public void setState(AggregateState state) {
        this.state = state;
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

    public LocalDateTime getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public Integer getSequence() {
        return sequence;
    }
    
    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }
    
    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Integer getOptionKey() {
        return optionKey;
    }
    
    public void setOptionKey(Integer optionKey) {
        this.optionKey = optionKey;
    }
}