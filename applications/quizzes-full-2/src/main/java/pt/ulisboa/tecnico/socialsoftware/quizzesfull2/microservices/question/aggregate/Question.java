package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.subscribe.QuestionSubscribesDeleteTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.subscribe.QuestionSubscribesUpdateTopic;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "questions")
public abstract class Question extends Aggregate {
    private String title;
    private String content;
    private LocalDateTime creationDate;
    private final Integer courseAggregateId;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Option> options = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionTopic> topics = new ArrayList<>();

    public Question() {
        this.courseAggregateId = null;
    }

    public Question(Integer aggregateId, Integer courseAggregateId, String title, String content,
                    LocalDateTime creationDate) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.courseAggregateId = courseAggregateId;
        this.title = title;
        this.content = content;
        this.creationDate = creationDate;
    }

    public Question(Question other) {
        super(other);
        this.courseAggregateId = other.getCourseAggregateId();
        this.title = other.getTitle();
        this.content = other.getContent();
        this.creationDate = other.getCreationDate();
        this.options = other.getOptions().stream()
                .map(Option::new)
                .collect(Collectors.toList());
        this.topics = other.getTopics().stream()
                .map(QuestionTopic::new)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyInvariants() {
        topicBelongsToQuestionCourse();
    }

    private void topicBelongsToQuestionCourse() {
        boolean allTopicsInCourse = this.topics.stream()
                .allMatch(topic -> this.courseAggregateId.equals(topic.getCourseAggregateId()));
        if (!allTopicsInCourse) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOPIC_BELONGS_TO_QUESTION_COURSE);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (getState() == AggregateState.ACTIVE) {
            for (QuestionTopic topic : this.topics) {
                eventSubscriptions.add(new QuestionSubscribesUpdateTopic(topic));
                eventSubscriptions.add(new QuestionSubscribesDeleteTopic(topic));
            }
        }
        return eventSubscriptions;
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

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public void addOption(Option option) {
        this.options.add(option);
    }

    public void removeOption(Integer optionKey) {
        this.options.removeIf(option -> optionKey.equals(option.getOptionKey()));
    }

    public List<QuestionTopic> getTopics() {
        return topics;
    }

    public void setTopics(List<QuestionTopic> topics) {
        this.topics = topics;
    }

    public void addTopic(QuestionTopic topic) {
        this.topics.add(topic);
    }

    public void removeTopic(Integer topicAggregateId) {
        this.topics.removeIf(topic -> topicAggregateId.equals(topic.getTopicAggregateId()));
    }
}
