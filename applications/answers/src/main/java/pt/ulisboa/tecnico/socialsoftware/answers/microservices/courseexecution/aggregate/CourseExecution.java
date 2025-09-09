package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Entity
public class CourseExecution extends Aggregate {
    @Id
    private String name;
    private String acronym;
    private String academicTerm;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Object course;
    private Object students; 

    public CourseExecution(String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, Object course, Object students) {
        this.name = name;
        this.acronym = acronym;
        this.academicTerm = academicTerm;
        this.startDate = startDate;
        this.endDate = endDate;
        this.course = course;
        this.students = students;
    }

    public CourseExecution(CourseExecution other) {
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

    public String getAcademicTerm() {
        return academicTerm;
    }

    public void setAcademicTerm(String academicTerm) {
        this.academicTerm = academicTerm;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Object getCourse() {
        return course;
    }

    public void setCourse(Object course) {
        this.course = course;
    }

    public Object getStudents() {
        return students;
    }

    public void setStudents(Object students) {
        this.students = students;
    }
	public Object createCourseExecution(String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, Object course, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getCourseExecutionById(Integer courseExecutionId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getAllCourseExecutions(UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getCourseExecutionsByCourse(Integer courseId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object enrollStudent(Integer courseExecutionId, Integer studentId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object unenrollStudent(Integer courseExecutionId, Integer studentId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object updateCourseExecution(Integer courseExecutionId, String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object deleteCourseExecution(Integer courseExecutionId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}