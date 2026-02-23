package pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;

public class TeacherDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String email;
    private String department;

    public TeacherDto() {
    }

    public TeacherDto(Teacher teacher) {
        this.aggregateId = teacher.getAggregateId();
        this.version = teacher.getVersion();
        this.state = teacher.getState();
        this.name = teacher.getName();
        this.email = teacher.getEmail();
        this.department = teacher.getDepartment();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}