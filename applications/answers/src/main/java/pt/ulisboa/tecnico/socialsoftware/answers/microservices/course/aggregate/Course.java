package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Entity
public class Course extends Aggregate {
    @Id
    private String name;
    private String acronym;
    private String courseType;
    private LocalDateTime creationDate; 

    public Course(String name, String acronym, String courseType, LocalDateTime creationDate) {
        this.name = name;
        this.acronym = acronym;
        this.courseType = courseType;
        this.creationDate = creationDate;
    }

    public Course(Course other) {
        // Copy constructor
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
	public Object createCourse(String name, String acronym, String courseType, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getCourseById(Integer courseId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getAllCourses(UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object updateCourse(Integer courseId, String name, String acronym, String courseType, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object deleteCourse(Integer courseId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}