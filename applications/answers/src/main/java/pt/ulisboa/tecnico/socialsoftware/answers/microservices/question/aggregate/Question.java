package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

@Entity
public abstract class Question extends Aggregate {
    private String title;
    private String content;
    private LocalDateTime creationDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "question")
    private QuestionCourse course;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "question")
    private Set<QuestionTopic> topics = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "question")
    private List<Option> options = new ArrayList<>();

    public Question() {

    }

    public Question(Integer aggregateId, QuestionDto questionDto, QuestionCourse course) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setCreationDate(questionDto.getCreationDate());
        setCourse(course);
    }

    public Question(Question other) {
        super(other);
        setTitle(other.getTitle());
        setContent(other.getContent());
        setCreationDate(other.getCreationDate());
        setCourse(new QuestionCourse(other.getCourse()));
        setTopics(other.getTopics().stream().map(QuestionTopic::new).collect(Collectors.toSet()));
        setOptions(other.getOptions().stream().map(Option::new).collect(Collectors.toList()));
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
        if (this.course != null) {
            this.course.setQuestion(this);
        }
    }

    public Set<QuestionTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<QuestionTopic> topics) {
        this.topics = topics;
        if (this.topics != null) {
            this.topics.forEach(item -> item.setQuestion(this));
        }
    }

    public void addQuestionTopic(QuestionTopic questionTopic) {
        if (this.topics == null) {
            this.topics = new HashSet<>();
        }
        this.topics.add(questionTopic);
        if (questionTopic != null) {
            questionTopic.setQuestion(this);
        }
    }

    public void removeQuestionTopic(Integer id) {
        if (this.topics != null) {
            this.topics.removeIf(item -> 
                item.getTopicId() != null && item.getTopicId().equals(id));
        }
    }

    public boolean containsQuestionTopic(Integer id) {
        if (this.topics == null) {
            return false;
        }
        return this.topics.stream().anyMatch(item -> 
            item.getTopicId() != null && item.getTopicId().equals(id));
    }

    public QuestionTopic findQuestionTopicById(Integer id) {
        if (this.topics == null) {
            return null;
        }
        return this.topics.stream()
            .filter(item -> item.getTopicId() != null && item.getTopicId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
        if (this.options != null) {
            this.options.forEach(item -> item.setQuestion(this));
        }
    }

    public void addOption(Option option) {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        this.options.add(option);
        if (option != null) {
            option.setQuestion(this);
        }
    }

    public void removeOption(Integer id) {
        if (this.options != null) {
            this.options.removeIf(item -> 
                item.getKey() != null && item.getKey().equals(id));
        }
    }

    public boolean containsOption(Integer id) {
        if (this.options == null) {
            return false;
        }
        return this.options.stream().anyMatch(item -> 
            item.getKey() != null && item.getKey().equals(id));
    }

    public Option findOptionById(Integer id) {
        if (this.options == null) {
            return null;
        }
        return this.options.stream()
            .filter(item -> item.getKey() != null && item.getKey().equals(id))
            .findFirst()
            .orElse(null);
    }


    // ============================================================================
    // INVARIANTS
    // ============================================================================

    public boolean invariantTitleNotEmpty() {
        return this.title.length() > 0;
    }

    public boolean invariantContentNotEmpty() {
        return this.content.length() > 0;
    }

    public boolean invariantNumberOfOptionsPositive() {
        return numberOfOptions > 0;
    }

    public boolean invariantCorrectOptionValid() {
        return correctOption >= 1 && correctOption <= numberOfOptions;
    }

    public boolean invariantOrderPositive() {
        return order > 0;
    }

    public boolean invariantUniqueTopics() {
        return this.topics.stream().map(item -> item.get${capitalize(topicId)}()).distinct().count() == this.topics.size();
    }

    public boolean invariantUniqueOptions() {
        return this.options.stream().map(item -> item.get${capitalize(optionNumber)}()).distinct().count() == this.options.size();
    }

    public boolean invariantOptionsMatchNumberOfOptions() {
        return numberOfOptions > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!(invariantTitleNotEmpty()
               && invariantContentNotEmpty()
               && invariantNumberOfOptionsPositive()
               && invariantCorrectOptionValid()
               && invariantOrderPositive()
               && invariantUniqueTopics()
               && invariantUniqueOptions()
               && invariantOptionsMatchNumberOfOptions())) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }

}