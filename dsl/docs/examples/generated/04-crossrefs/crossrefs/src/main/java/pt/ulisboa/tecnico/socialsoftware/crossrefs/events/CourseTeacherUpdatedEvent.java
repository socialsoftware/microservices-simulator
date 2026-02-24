package pt.ulisboa.tecnico.socialsoftware.crossrefs.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CourseTeacherUpdatedEvent extends Event {
    private Integer teacherAggregateId;
    private Integer teacherVersion;
    private String teacherName;
    private String teacherEmail;
    private String teacherDepartment;

    public CourseTeacherUpdatedEvent() {
        super();
    }

    public CourseTeacherUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public CourseTeacherUpdatedEvent(Integer aggregateId, Integer teacherAggregateId, Integer teacherVersion, String teacherName, String teacherEmail, String teacherDepartment) {
        super(aggregateId);
        setTeacherAggregateId(teacherAggregateId);
        setTeacherVersion(teacherVersion);
        setTeacherName(teacherName);
        setTeacherEmail(teacherEmail);
        setTeacherDepartment(teacherDepartment);
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

}