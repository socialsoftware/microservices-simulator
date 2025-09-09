package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Entity
public class Quiz extends Aggregate {
    @Id
    private String title;
    private String description;
    private String quizType;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private Integer numberOfQuestions;
    private Object courseExecution;
    private Object questions;
    private Object options; 

    public Quiz(String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions, Object courseExecution, Object questions, Object options) {
        this.title = title;
        this.description = description;
        this.quizType = quizType;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.numberOfQuestions = numberOfQuestions;
        this.courseExecution = courseExecution;
        this.questions = questions;
        this.options = options;
    }

    public Quiz(Quiz other) {
        // Copy constructor
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuizType() {
        return quizType;
    }

    public void setQuizType(String quizType) {
        this.quizType = quizType;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Object getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(Object courseExecution) {
        this.courseExecution = courseExecution;
    }

    public Object getQuestions() {
        return questions;
    }

    public void setQuestions(Object questions) {
        this.questions = questions;
    }

    public Object getOptions() {
        return options;
    }

    public void setOptions(Object options) {
        this.options = options;
    }
	public Object createQuiz(String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions, Object courseExecution, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getQuizById(Integer quizId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getAllQuizzes(UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getQuizzesByCourseExecution(Integer courseExecutionId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getQuizzesByType(String quizType, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object updateQuiz(Integer quizId, String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object deleteQuiz(Integer quizId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}