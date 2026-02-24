package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

@Entity
public class CourseTeacher {
    @Id
    @GeneratedValue
    private Long id;
    private String teacherName;
    private String teacherEmail;
    private String teacherDepartment;
    private Integer teacherAggregateId;
    private Integer teacherVersion;
    private AggregateState teacherState;
    @OneToOne
    private Course course;

    public CourseTeacher() {

    }

    public CourseTeacher(TeacherDto teacherDto) {
        setTeacherAggregateId(teacherDto.getAggregateId());
        setTeacherVersion(teacherDto.getVersion());
        setTeacherState(teacherDto.getState());
    }

    public CourseTeacher(CourseTeacherDto courseTeacherDto) {
        setTeacherName(courseTeacherDto.getName());
        setTeacherEmail(courseTeacherDto.getEmail());
        setTeacherDepartment(courseTeacherDto.getDepartment());
        setTeacherAggregateId(courseTeacherDto.getAggregateId());
        setTeacherVersion(courseTeacherDto.getVersion());
        setTeacherState(courseTeacherDto.getState() != null ? AggregateState.valueOf(courseTeacherDto.getState()) : null);
    }

    public CourseTeacher(CourseTeacher other) {
        setTeacherName(other.getTeacherName());
        setTeacherEmail(other.getTeacherEmail());
        setTeacherDepartment(other.getTeacherDepartment());
        setTeacherAggregateId(other.getTeacherAggregateId());
        setTeacherVersion(other.getTeacherVersion());
        setTeacherState(other.getTeacherState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getTeacherEmail() {
        return teacherEmail;
    }

    public void setTeacherEmail(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    public String getTeacherDepartment() {
        return teacherDepartment;
    }

    public void setTeacherDepartment(String teacherDepartment) {
        this.teacherDepartment = teacherDepartment;
    }

    public Integer getTeacherAggregateId() {
        return teacherAggregateId;
    }

    public void setTeacherAggregateId(Integer teacherAggregateId) {
        this.teacherAggregateId = teacherAggregateId;
    }

    public Integer getTeacherVersion() {
        return teacherVersion;
    }

    public void setTeacherVersion(Integer teacherVersion) {
        this.teacherVersion = teacherVersion;
    }

    public AggregateState getTeacherState() {
        return teacherState;
    }

    public void setTeacherState(AggregateState teacherState) {
        this.teacherState = teacherState;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }




    public CourseTeacherDto buildDto() {
        CourseTeacherDto dto = new CourseTeacherDto();
        dto.setName(getTeacherName());
        dto.setEmail(getTeacherEmail());
        dto.setDepartment(getTeacherDepartment());
        dto.setAggregateId(getTeacherAggregateId());
        dto.setVersion(getTeacherVersion());
        dto.setState(getTeacherState() != null ? getTeacherState().name() : null);
        return dto;
    }
}