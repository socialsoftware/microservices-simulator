package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

@Entity
public class EnrollmentTeacher {
    @Id
    @GeneratedValue
    private Long id;
    private Integer teacherAggregateId;
    private Integer teacherVersion;
    private AggregateState teacherState;
    @OneToOne
    private Enrollment enrollment;

    public EnrollmentTeacher() {

    }

    public EnrollmentTeacher(TeacherDto teacherDto) {
        setTeacherAggregateId(teacherDto.getAggregateId());
        setTeacherVersion(teacherDto.getVersion());
        setTeacherState(teacherDto.getState());
    }

    public EnrollmentTeacher(EnrollmentTeacherDto enrollmentTeacherDto) {
        setTeacherAggregateId(enrollmentTeacherDto.getAggregateId());
        setTeacherVersion(enrollmentTeacherDto.getVersion());
        setTeacherState(enrollmentTeacherDto.getState());
    }

    public EnrollmentTeacher(EnrollmentTeacher other) {
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

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
    }




    public EnrollmentTeacherDto buildDto() {
        EnrollmentTeacherDto dto = new EnrollmentTeacherDto();
        dto.setAggregateId(getTeacherAggregateId());
        dto.setVersion(getTeacherVersion());
        dto.setState(getTeacherState());
        return dto;
    }
}