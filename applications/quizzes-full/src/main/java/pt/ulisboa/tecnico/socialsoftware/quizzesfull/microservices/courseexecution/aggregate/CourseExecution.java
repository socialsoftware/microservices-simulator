package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.*;

/*
    INTRA-INVARIANTS:
        COURSE_EXECUTION_ACRONYM_NOT_BLANK
        COURSE_EXECUTION_ACADEMIC_TERM_NOT_BLANK
        REMOVE_NO_STUDENTS
        STUDENT_ALREADY_ENROLLED
    INTER-INVARIANTS:
        (wired in Phase 4 via /wire-event)
*/
@Entity
public abstract class CourseExecution extends Aggregate {

    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;

    // --- Snapshot fields ---
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "courseExecution")
    private CourseExecutionCourse courseExecutionCourse;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "courseExecution")
    private Set<CourseExecutionStudent> students = new HashSet<>();

    public CourseExecution() {}

    public CourseExecution(Integer aggregateId, CourseExecutionDto dto, CourseExecutionCourse courseExecutionCourse) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAcronym(dto.getAcronym());
        setAcademicTerm(dto.getAcademicTerm());
        setEndDate(dto.getEndDate() != null ? DateHandler.toLocalDateTime(dto.getEndDate()) : null);
        setCourseExecutionCourse(courseExecutionCourse);
    }

    public CourseExecution(CourseExecution other) {
        super(other);
        setAcronym(other.getAcronym());
        setAcademicTerm(other.getAcademicTerm());
        setEndDate(other.getEndDate());
        setCourseExecutionCourse(new CourseExecutionCourse(other.getCourseExecutionCourse()));
        setStudents(other.getStudents().stream()
                .map(CourseExecutionStudent::new)
                .collect(Collectors.toSet()));
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    /*
     * COURSE_EXECUTION_ACRONYM_NOT_BLANK
     * CourseExecution.acronym must not be null or blank
     */
    private boolean invariantAcronymNotBlank() {
        return acronym != null && !acronym.isBlank();
    }

    /*
     * COURSE_EXECUTION_ACADEMIC_TERM_NOT_BLANK
     * CourseExecution.academicTerm must not be null or blank
     */
    private boolean invariantAcademicTermNotBlank() {
        return academicTerm != null && !academicTerm.isBlank();
    }

    /*
     * REMOVE_NO_STUDENTS
     * CourseExecution.state == DELETED ⟹ CourseExecution.students.isEmpty()
     */
    private boolean invariantRemoveNoStudents() {
        if (getState() == AggregateState.DELETED) {
            return students.isEmpty();
        }
        return true;
    }

    /*
     * STUDENT_ALREADY_ENROLLED
     * A User may appear in CourseExecution.students at most once
     */
    private boolean invariantStudentAlreadyEnrolled() {
        long distinctCount = students.stream()
                .map(CourseExecutionStudent::getUserAggregateId)
                .distinct()
                .count();
        return distinctCount == students.size();
    }

    @Override
    public void verifyInvariants() {
        if (getState() == ACTIVE) {
            if (!invariantAcronymNotBlank()) {
                throw new QuizzesFullException(COURSE_EXECUTION_MISSING_ACRONYM);
            }
            if (!invariantAcademicTermNotBlank()) {
                throw new QuizzesFullException(COURSE_EXECUTION_MISSING_ACADEMIC_TERM);
            }
            if (!invariantStudentAlreadyEnrolled()) {
                throw new QuizzesFullException(COURSE_EXECUTION_STUDENT_ALREADY_ENROLLED,
                        getAggregateId(), getAggregateId());
            }
        }
        if (!invariantRemoveNoStudents()) {
            throw new QuizzesFullException(REMOVE_NO_STUDENTS, getAggregateId());
        }
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public void setVersion(Long version) {
        if (this.courseExecutionCourse != null && this.courseExecutionCourse.getCourseVersion() == null) {
            this.courseExecutionCourse.setCourseVersion(version);
        }
        super.setVersion(version);
    }

    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }

    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public CourseExecutionCourse getCourseExecutionCourse() { return courseExecutionCourse; }
    public void setCourseExecutionCourse(CourseExecutionCourse courseExecutionCourse) {
        this.courseExecutionCourse = courseExecutionCourse;
        this.courseExecutionCourse.setCourseExecution(this);
    }

    public Set<CourseExecutionStudent> getStudents() { return students; }
    public void setStudents(Set<CourseExecutionStudent> students) {
        this.students = students;
        this.students.forEach(s -> s.setCourseExecution(this));
    }

    public void addStudent(CourseExecutionStudent student) {
        this.students.add(student);
        student.setCourseExecution(this);
    }

    public boolean hasStudent(Integer userAggregateId) {
        return students.stream().anyMatch(s -> s.getUserAggregateId().equals(userAggregateId));
    }

    public CourseExecutionStudent findStudent(Integer userAggregateId) {
        return students.stream()
                .filter(s -> s.getUserAggregateId().equals(userAggregateId))
                .findFirst()
                .orElse(null);
    }

    public void removeStudent(Integer userAggregateId) {
        CourseExecutionStudent toRemove = findStudent(userAggregateId);
        if (toRemove == null) {
            throw new QuizzesFullException(COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId, getAggregateId());
        }
        students.remove(toRemove);
    }
}
