package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuestionDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String content;
    private String creationDate;
    private CourseDto course;
    
    public QuestionDto() {
    }
    
    public QuestionDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question question) {
        // Standard aggregate fields
        setAggregateId(question.getAggregateId());
        setVersion(question.getVersion());
        setState(question.getState().toString());

        // Root entity fields
        setTitle(question.getTitle());
        setContent(question.getContent());
        setCreationDate(question.getCreationDate());

        // Fields from QuestionCourse
        setCourse(question.getQuestionCourse().getCourse());

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

    public String getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public CourseDto getCourse() {
        return course;
    }
    
    public void setCourse(CourseDto course) {
        this.course = course;
    }
}