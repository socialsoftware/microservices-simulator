package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@Entity
public abstract class Topic extends Aggregate {
    @Id
    private String name;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "topic")
    private TopicCourse course;
    private LocalDateTime creationDate; 

    public Topic() {
    }

    public Topic(Integer aggregateId, TopicDto topicDto, TopicCourse course) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(topicDto.getName());
        setCourse(course);
        setCreationDate(topicDto.getCreationDate());
    }

    public Topic(Topic other) {
        super(other);
        setName(other.getName());
        setCourse(new TopicCourse(other.getCourse()));
        setCreationDate(other.getCreationDate());
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicCourse getCourse() {
        return course;
    }

    public void setCourse(TopicCourse course) {
        this.course = course;
        if (this.course != null) {
            this.course.setTopic(this);
        }
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

	public void createTopic(String name, TopicCourse course, UnitOfWork unitOfWork) {

	}

	public void getTopicById(Integer topicId, UnitOfWork unitOfWork) {

	}

	public void getAllTopics(UnitOfWork unitOfWork) {

	}

	public void getTopicsByCourse(Integer courseId, UnitOfWork unitOfWork) {

	}

	public void updateTopic(Integer topicId, String name, UnitOfWork unitOfWork) {

	}

	public void deleteTopic(Integer topicId, UnitOfWork unitOfWork) {

	}

}