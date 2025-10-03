package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;

@Entity
public abstract class Course extends Aggregate {
    @Id
    private String name;
    private String acronym;
    @Enumerated(EnumType.STRING)
    private CourseType courseType;
    private LocalDateTime creationDate; 

    public Course() {
    }

    public Course(Integer aggregateId, CourseDto courseDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(courseDto.getName());
        setAcronym(courseDto.getAcronym());
        setCourseType(courseDto.getCourseType());
        setCreationDate(courseDto.getCreationDate());
    }

    public Course(Course other) {
        super(other);
        setName(other.getName());
        setAcronym(other.getAcronym());
        setCourseType(other.getCourseType());
        setCreationDate(other.getCreationDate());
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

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

	public Course createCourse(String name, String acronym, String courseType, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Course getCourseById(Integer courseId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public List<Course> getAllCourses(UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Course updateCourse(Integer courseId, String name, String acronym, String courseType, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public void deleteCourse(Integer courseId, UnitOfWork unitOfWork) {

	}

}