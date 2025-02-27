package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.subscribe.QuestionSubscribesDeleteTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.subscribe.QuestionSubscribesUpdateTopic;

/*
    INTRA-INVARIANTS:

    INTER-INVARIANTS:
        TOPICS_EXIST
        COURSE_EXISTS (course does not send events)
        COURSE_SAME_TOPIC_COURSE ()
 */
@Entity
public abstract class Question extends Aggregate {
    private String title;
    private String content;
    private LocalDateTime creationDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "question")
    private QuestionCourse questionCourse;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "question")
    private Set<QuestionTopic> questionTopics = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "question")
    private List<Option> options = new ArrayList<>();

    public Question() {

    }

    public Question(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setCreationDate(DateHandler.now());
        setQuestionCourse(questionCourse);
        setOptions(questionDto.getOptionDtos().stream().map(Option::new).collect(Collectors.toList()));

        Integer optionKeyGenerator = 1;
        for (Option o : getOptions()) {
            o.setOptionKey(optionKeyGenerator++);
        }

        setQuestionTopics(new HashSet<>(questionTopics));
    }

    public Question(Question other) {
        super(other);
        setTitle(other.getTitle());
        setContent(other.getContent());
        setCreationDate(other.getCreationDate());
        setQuestionCourse(new QuestionCourse(other.getQuestionCourse()));
        setOptions(other.getOptions().stream().map(Option::new).collect(Collectors.toList()));
        setQuestionTopics(other.getQuestionTopics().stream().map(QuestionTopic::new).collect(Collectors.toSet()));
    }

    @Override
    public void verifyInvariants() {
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        //return Set.of(UPDATE_TOPIC, DELETE_TOPIC);
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (getState() == ACTIVE) {
            interInvariantTopicsExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }

    private void interInvariantTopicsExist(Set<EventSubscription> eventSubscriptions) {
        for (QuestionTopic topic : this.questionTopics) {
            eventSubscriptions.add(new QuestionSubscribesDeleteTopic(topic));
            eventSubscriptions.add(new QuestionSubscribesUpdateTopic(topic));
        }
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

    public QuestionCourse getQuestionCourse() {
        return questionCourse;
    }

    public void setQuestionCourse(QuestionCourse course) {
        this.questionCourse = course;
        this.questionCourse.setQuestion(this);
    }

    public Set<QuestionTopic> getQuestionTopics() {
        return questionTopics;
    }

    public void setQuestionTopics(Set<QuestionTopic> topics) {
        this.questionTopics = topics;
        this.questionTopics.forEach(questionTopic -> questionTopic.setQuestion(this));
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
        this.options.forEach(option -> option.setQuestion(this));
    }

    public void update(QuestionDto questionDto) {
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setOptions(questionDto.getOptionDtos().stream().map(Option::new).collect(Collectors.toList()));
    }

    public QuestionTopic findTopic(Integer topicAggregateId) {
        for(QuestionTopic questionTopic : this.questionTopics) {
            if(questionTopic.getTopicAggregateId().equals(topicAggregateId)) {
                return questionTopic;
            }
        }
        return null;
    }
}
