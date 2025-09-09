package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Entity
public class Question extends Aggregate {
    @Id
    private String title;
    private String content;
    private Integer numberOfOptions;
    private Integer correctOption;
    private Integer order;
    private Object course;
    private Object topics;
    private Object options; 

    public Question(String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, Object course, Object topics, Object options) {
        this.title = title;
        this.content = content;
        this.numberOfOptions = numberOfOptions;
        this.correctOption = correctOption;
        this.order = order;
        this.course = course;
        this.topics = topics;
        this.options = options;
    }

    public Question(Question other) {
        // Copy constructor
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

    public Object getCourse() {
        return course;
    }

    public void setCourse(Object course) {
        this.course = course;
    }

    public Object getTopics() {
        return topics;
    }

    public void setTopics(Object topics) {
        this.topics = topics;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }
	public Object createQuestion(String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, Object course, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getQuestionById(Integer questionId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getAllQuestions(UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getQuestionsByCourse(Integer courseId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getQuestionsByTopic(Integer topicId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object updateQuestion(Integer questionId, String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object deleteQuestion(Integer questionId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}