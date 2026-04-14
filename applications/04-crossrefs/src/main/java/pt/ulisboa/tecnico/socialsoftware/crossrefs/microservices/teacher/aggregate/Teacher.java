package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Teacher extends Aggregate {
    private String name;
    private String email;
    private String department;

    public Teacher() {

    }

    public Teacher(Integer aggregateId, TeacherDto teacherDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(teacherDto.getName());
        setEmail(teacherDto.getEmail());
        setDepartment(teacherDto.getDepartment());
    }


    public Teacher(Teacher other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());
        setDepartment(other.getDepartment());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }



    private boolean invariantRule0() {
        return this.name != null && this.name.length() > 0;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Teacher name cannot be blank");
        }
    }

    public TeacherDto buildDto() {
        TeacherDto dto = new TeacherDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setEmail(getEmail());
        dto.setDepartment(getDepartment());
        return dto;
    }
}