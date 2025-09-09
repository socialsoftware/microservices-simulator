package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Entity
public class Topic extends Aggregate {
    @Id
    private String name;
    private Object course;
    private LocalDateTime creationDate; 

    public Topic(String name, Object course, LocalDateTime creationDate) {
        this.name = name;
        this.course = course;
        this.creationDate = creationDate;
    }

    public Topic(Topic other) {
        // Copy constructor
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getCourse() {
        return course;
    }

    public void setCourse(Object course) {
        this.course = course;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
	public Object createTopic(String name, Object course, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getTopicById(Integer topicId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getAllTopics(UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getTopicsByCourse(Integer courseId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object updateTopic(Integer topicId, String name, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object deleteTopic(Integer topicId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}