package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuestionCreatedEvent extends Event {
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private QuestionCourse course;
    private Set<QuestionTopic> topics;
    private List<Option> options;

    public QuestionCreatedEvent() {
    }

    public QuestionCreatedEvent(Integer aggregateId, String title, String content, LocalDateTime creationDate, QuestionCourse course, Set<QuestionTopic> topics, List<Option> options) {
        super(aggregateId);
        setTitle(title);
        setContent(content);
        setCreationDate(creationDate);
        setCourse(course);
        setTopics(topics);
        setOptions(options);
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

    public QuestionCourse getCourse() {
        return course;
    }

    public void setCourse(QuestionCourse course) {
        this.course = course;
    }

    public Set<QuestionTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<QuestionTopic> topics) {
        this.topics = topics;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

}