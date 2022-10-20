package pt.ulisboa.tecnico.socialsoftware.blcm.question.domain;

import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.blcm.question.dto.QuestionDto;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.AggregateType.QUESTION;
import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.EventType.DELETE_TOPIC;
import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.EventType.UPDATE_TOPIC;

/*
    INTRA-INVARIANTS:

    INTER-INVARIANTS:
        TOPICS_EXIST
        COURSE_EXISTS (course does not send events)
        COURSE_SAME_TOPIC_COURSE ()
 */
@Entity
@Table(name = "questions")
public class Question extends Aggregate {

    @Column
    private String title;

    @Column
    private String content;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Embedded
    private QuestionCourse course;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<QuestionTopic> topics;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Option> options;

    public Question() {

    }

    public Question(Integer aggregateId, QuestionCourse course, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        super(aggregateId, QUESTION);
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setCreationDate(LocalDateTime.now());
        setCourse(course);
        setOptions(questionDto.getOptionDtos().stream().map(Option::new).collect(Collectors.toList()));

        Integer optionKeyGenerator = 1;
        for(Option o : getOptions()) {
            o.setKey(optionKeyGenerator++);
        }

        setTopics(new HashSet<>(questionTopics));
        setPrev(null);
    }

    public Question(Question other) {
        super(other);
        setTitle(other.getTitle());
        setContent(other.getContent());
        setCreationDate(other.getCreationDate());
        setCourse(new QuestionCourse(other.getCourse()));
        setOptions(other.getOptions().stream().map(Option::new).collect(Collectors.toList()));
        setTopics(other.getTopics().stream().map(QuestionTopic::new).collect(Collectors.toSet()));
    }


    @Override
    public void verifyInvariants() {

    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        //return Set.of(UPDATE_TOPIC, DELETE_TOPIC);
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if(getState() == ACTIVE) {
            for (QuestionTopic topic : this.topics) {
                interInvariantTopicsExist(eventSubscriptions, topic);
            }
        }
        return eventSubscriptions;
    }

    private static void interInvariantTopicsExist(Set<EventSubscription> eventSubscriptions, QuestionTopic topic) {
        eventSubscriptions.add(new EventSubscription(topic.getAggregateId(), topic.getVersion(), DELETE_TOPIC));
        eventSubscriptions.add(new EventSubscription(topic.getAggregateId(), topic.getVersion(), UPDATE_TOPIC));
    }

    @Override
    public Set<String> getFieldsChangedByFunctionalities() {
        return Set.of("title", "content", "options", "topics");
    }

    @Override
    public Set<String[]> getIntentions() {

        return Set.of(
                new String[]{"title", "content"},
                new String[]{"title", "options"},
                new String[]{"content", "options"}
        );
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        return null;
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

    public void update(QuestionDto questionDto) {
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setOptions(questionDto.getOptionDtos().stream().map(Option::new).collect(Collectors.toList()));
    }

    public QuestionTopic findTopic(Integer topicAggregateId) {
        for(QuestionTopic questionTopic : this.topics) {
            if(questionTopic.getAggregateId().equals(topicAggregateId)) {
                return questionTopic;
            }
        }
        return null;
    }
}
