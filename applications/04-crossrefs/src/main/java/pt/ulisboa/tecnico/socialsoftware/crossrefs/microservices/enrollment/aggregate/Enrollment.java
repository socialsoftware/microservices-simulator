package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.subscribe.EnrollmentSubscribesCourseDeletedCourseExists;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentCourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Enrollment extends Aggregate {
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "enrollment")
    private EnrollmentCourse course;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "enrollment")
    private Set<EnrollmentTeacher> teachers = new HashSet<>();
    private LocalDateTime enrollmentDate;
    private Boolean active;

    public Enrollment() {

    }

    public Enrollment(Integer aggregateId, EnrollmentDto enrollmentDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setEnrollmentDate(enrollmentDto.getEnrollmentDate());
        setActive(enrollmentDto.getActive());
        setCourse(enrollmentDto.getCourse() != null ? new EnrollmentCourse(enrollmentDto.getCourse()) : null);
        setTeachers(enrollmentDto.getTeachers() != null ? enrollmentDto.getTeachers().stream().map(EnrollmentTeacher::new).collect(Collectors.toSet()) : null);
    }


    public Enrollment(Enrollment other) {
        super(other);
        setCourse(other.getCourse() != null ? new EnrollmentCourse(other.getCourse()) : null);
        setTeachers(other.getTeachers() != null ? other.getTeachers().stream().map(EnrollmentTeacher::new).collect(Collectors.toSet()) : null);
        setEnrollmentDate(other.getEnrollmentDate());
        setActive(other.getActive());
    }

    public EnrollmentCourse getCourse() {
        return course;
    }

    public void setCourse(EnrollmentCourse course) {
        this.course = course;
        if (this.course != null) {
            this.course.setEnrollment(this);
        }
    }

    public Set<EnrollmentTeacher> getTeachers() {
        return teachers;
    }

    public void setTeachers(Set<EnrollmentTeacher> teachers) {
        this.teachers = teachers;
        if (this.teachers != null) {
            this.teachers.forEach(item -> item.setEnrollment(this));
        }
    }

    public void addEnrollmentTeacher(EnrollmentTeacher enrollmentTeacher) {
        if (this.teachers == null) {
            this.teachers = new HashSet<>();
        }
        this.teachers.add(enrollmentTeacher);
        if (enrollmentTeacher != null) {
            enrollmentTeacher.setEnrollment(this);
        }
    }

    public void removeEnrollmentTeacher(Long id) {
        if (this.teachers != null) {
            this.teachers.removeIf(item -> 
                item.getId() != null && item.getId().equals(id));
        }
    }

    public boolean containsEnrollmentTeacher(Long id) {
        if (this.teachers == null) {
            return false;
        }
        return this.teachers.stream().anyMatch(item -> 
            item.getId() != null && item.getId().equals(id));
    }

    public EnrollmentTeacher findEnrollmentTeacherById(Long id) {
        if (this.teachers == null) {
            return null;
        }
        return this.teachers.stream()
            .filter(item -> item.getId() != null && item.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCourseExists(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantCourseExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new EnrollmentSubscribesCourseDeletedCourseExists(this.getCourse()));
    }


    private boolean invariantRule0() {
        return this.course != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Enrollment must have a course");
        }
    }

    public EnrollmentDto buildDto() {
        EnrollmentDto dto = new EnrollmentDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setCourse(getCourse() != null ? new EnrollmentCourseDto(getCourse()) : null);
        dto.setTeachers(getTeachers() != null ? getTeachers().stream().map(EnrollmentTeacher::buildDto).collect(Collectors.toSet()) : null);
        dto.setEnrollmentDate(getEnrollmentDate());
        dto.setActive(getActive());
        return dto;
    }
}