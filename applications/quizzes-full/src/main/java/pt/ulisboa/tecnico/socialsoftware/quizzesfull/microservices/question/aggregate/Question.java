package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        (none)
    INTER-INVARIANTS:
        TOPICS_EXIST (subscribes to UpdateTopicEvent, DeleteTopicEvent)
        TOPIC_BELONGS_TO_QUESTION_COURSE (P2 — enforced at saga level)
 */
@Entity
public abstract class Question extends Aggregate {

    @Column
    private String title;

    @Column
    private String content;

    @Column
    private LocalDateTime creationDate;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "question")
    private QuestionCourse questionCourse;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Option> options = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionTopic> topics = new HashSet<>();

    public Question() {}

    public Question(Integer aggregateId, String title, String content, QuestionCourse questionCourse, Set<Option> options, Set<QuestionTopic> topics) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(title);
        setContent(content);
        setCreationDate(LocalDateTime.now());
        setQuestionCourse(questionCourse);
        setOptions(options);
        setTopics(topics);
    }

    public Question(Question other) {
        super(other);
        setTitle(other.getTitle());
        setContent(other.getContent());
        setCreationDate(other.getCreationDate());
        setQuestionCourse(new QuestionCourse(other.getQuestionCourse()));
        for (Option option : other.getOptions()) {
            this.options.add(new Option(option));
        }
        for (QuestionTopic topic : other.getTopics()) {
            this.topics.add(new QuestionTopic(topic));
        }
    }

    @Override
    public void verifyInvariants() {}

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public QuestionCourse getQuestionCourse() { return questionCourse; }
    public void setQuestionCourse(QuestionCourse questionCourse) {
        this.questionCourse = questionCourse;
        this.questionCourse.setQuestion(this);
    }

    public Set<Option> getOptions() { return options; }
    public void setOptions(Set<Option> options) { this.options = options; }
    public void addOption(Option option) { this.options.add(option); }

    public Set<QuestionTopic> getTopics() { return topics; }
    public void setTopics(Set<QuestionTopic> topics) { this.topics = topics; }
    public void addTopic(QuestionTopic topic) { this.topics.add(topic); }
    public void removeTopic(QuestionTopic topic) { this.topics.remove(topic); }
}
