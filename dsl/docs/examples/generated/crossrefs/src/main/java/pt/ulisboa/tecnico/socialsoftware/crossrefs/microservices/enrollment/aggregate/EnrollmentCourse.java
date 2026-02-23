package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentCourseDto;

@Entity
public class EnrollmentCourse {
    @Id
    @GeneratedValue
    private Long id;
    private String courseTitle;
    private String courseDescription;
    private Integer courseMaxStudents;
    private Integer courseAggregateId;
    private Integer courseVersion;
    private AggregateState courseState;
    @OneToOne
    private Enrollment enrollment;

    public EnrollmentCourse() {

    }

    public EnrollmentCourse(CourseDto courseDto) {
        setCourseAggregateId(courseDto.getAggregateId());
        setCourseVersion(courseDto.getVersion());
        setCourseState(courseDto.getState());
    }

    public EnrollmentCourse(EnrollmentCourseDto enrollmentCourseDto) {
        setCourseTitle(enrollmentCourseDto.getTitle());
        setCourseDescription(enrollmentCourseDto.getDescription());
        setCourseMaxStudents(enrollmentCourseDto.getMaxStudents());
        setCourseAggregateId(enrollmentCourseDto.getAggregateId());
        setCourseVersion(enrollmentCourseDto.getVersion());
        setCourseState(enrollmentCourseDto.getState() != null ? AggregateState.valueOf(enrollmentCourseDto.getState()) : null);
    }

    public EnrollmentCourse(EnrollmentCourse other) {
        setCourseTitle(other.getCourseTitle());
        setCourseDescription(other.getCourseDescription());
        setCourseMaxStudents(other.getCourseMaxStudents());
        setCourseAggregateId(other.getCourseAggregateId());
        setCourseVersion(other.getCourseVersion());
        setCourseState(other.getCourseState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public Integer getCourseMaxStudents() {
        return courseMaxStudents;
    }

    public void setCourseMaxStudents(Integer courseMaxStudents) {
        this.courseMaxStudents = courseMaxStudents;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }

    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public AggregateState getCourseState() {
        return courseState;
    }

    public void setCourseState(AggregateState courseState) {
        this.courseState = courseState;
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }




    public EnrollmentCourseDto buildDto() {
        EnrollmentCourseDto dto = new EnrollmentCourseDto();
        dto.setTitle(getCourseTitle());
        dto.setDescription(getCourseDescription());
        dto.setMaxStudents(getCourseMaxStudents());
        dto.setAggregateId(getCourseAggregateId());
        dto.setVersion(getCourseVersion());
        dto.setState(getCourseState() != null ? getCourseState().name() : null);
        return dto;
    }
}