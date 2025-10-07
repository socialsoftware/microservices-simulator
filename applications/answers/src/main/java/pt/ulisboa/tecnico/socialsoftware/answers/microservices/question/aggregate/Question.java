package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

@Entity
public abstract class Question extends Aggregate {
    @Id
    private String title;
    private String content;
    private Integer numberOfOptions;
    private Integer correctOption;
    private Integer order;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "question")
    private QuestionCourse course;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "question")
    private Set<QuestionTopic> topics = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "question")
    private Set<Option> options = new HashSet<>(); 

    public Question() {
    }

    public Question(Integer aggregateId, QuestionDto questionDto, QuestionCourse course) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setNumberOfOptions(questionDto.getNumberOfOptions());
        setCorrectOption(questionDto.getCorrectOption());
        setOrder(questionDto.getOrder());
        setCourse(course);
    }

    public Question(Question other) {
        super(other);
        setTitle(other.getTitle());
        setContent(other.getContent());
        setNumberOfOptions(other.getNumberOfOptions());
        setCorrectOption(other.getCorrectOption());
        setOrder(other.getOrder());
        setCourse(new QuestionCourse(other.getCourse()));
        setTopics(other.getTopics().stream().map(QuestionTopic::new).collect(Collectors.toSet()));
        setOptions(other.getOptions().stream().map(Option::new).collect(Collectors.toSet()));
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
            this.topics.forEach(questiontopic -> questiontopic.setQuestion(this));
        }
    }

    public Set<Option> getOptions() {
        return options;
    }

    public void setOptions(Set<Option> options) {
        this.options = options;
        if (this.options != null) {
            this.options.forEach(option -> option.setQuestion(this));
        }
    }

	public void createQuestion(String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, QuestionCourse course, UnitOfWork unitOfWork) {

	}

	public void getQuestionById(Integer questionId, UnitOfWork unitOfWork) {

	}

	public void getAllQuestions(UnitOfWork unitOfWork) {

	}

	public void getQuestionsByCourse(Integer courseId, UnitOfWork unitOfWork) {

	}

	public void getQuestionsByTopic(Integer topicId, UnitOfWork unitOfWork) {

	}

	public void updateQuestion(Integer questionId, String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, UnitOfWork unitOfWork) {

	}

	public void deleteQuestion(Integer questionId, UnitOfWork unitOfWork) {

	}

}