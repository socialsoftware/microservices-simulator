package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.subscribe.CourseExecutionSubscribesRemoveUser;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.subscribe.ExecutionSubscribesCreateQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.subscribe.ExecutionSubscribesDeleteQuestion;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;

/*
    INTRA-INVARIANTS
        REMOVE_NO_STUDENTS
        ALL_STUDENTS_ARE_ACTIVE
        CANNOT_REMOVE_IF_STUDENTS
        NO_DUPLICATE_COURSE_EXECUTION
    INTER-INVARIANTS
        USER_EXISTS
        COURSE_EXISTS (does it count? course doesn't send events)
        CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT (needs service-level context to know if this is the last execution)
 */

@Entity
public abstract class Execution extends Aggregate {
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "execution")
    private CourseExecutionCourse courseExecutionCourse;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "execution")
    private Set<CourseExecutionStudent> students = new HashSet<>();
    private int courseQuestionCount = 0;

    public Execution() {
    }

    public Execution(Integer aggregateId, CourseExecutionDto courseExecutionDto,
            CourseExecutionCourse courseExecutionCourse) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAcronym(courseExecutionDto.getAcronym());
        setAcademicTerm(courseExecutionDto.getAcademicTerm());
        setEndDate(DateHandler.toLocalDateTime(courseExecutionDto.getEndDate()));
        setExecutionCourse(courseExecutionCourse);
    }

    public Execution(Execution other) {
        super(other);
        setAcronym(other.getAcronym());
        setAcademicTerm(other.getAcademicTerm());
        setEndDate(other.getEndDate());
        setExecutionCourse(new CourseExecutionCourse(other.getExecutionCourse()));
        setStudents(other.getStudents().stream().map(CourseExecutionStudent::new).collect(Collectors.toSet()));
        this.courseQuestionCount = other.getCourseQuestionCount();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (getState() == ACTIVE) {
            interInvariantUsersExist(eventSubscriptions);
            interInvariantCourseHasNoContent(eventSubscriptions);
        }
        return eventSubscriptions;
    }

    private void interInvariantCourseHasNoContent(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new ExecutionSubscribesCreateQuestion(this.courseExecutionCourse));
        eventSubscriptions.add(new ExecutionSubscribesDeleteQuestion(this.courseExecutionCourse));
    }

    private void interInvariantUsersExist(Set<EventSubscription> eventSubscriptions) {
        for (CourseExecutionStudent student : this.students) {
            eventSubscriptions.add(new CourseExecutionSubscribesRemoveUser(student));
        }
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

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public CourseExecutionCourse getExecutionCourse() {
        return courseExecutionCourse;
    }

    public void setExecutionCourse(CourseExecutionCourse course) {
        this.courseExecutionCourse = course;
        this.courseExecutionCourse.setCourseExecution(this);
    }

    public Set<CourseExecutionStudent> getStudents() {
        return students;
    }

    public void setStudents(Set<CourseExecutionStudent> students) {
        this.students = students;
        this.students.forEach(courseExecutionStudent -> courseExecutionStudent.setCourseExecution(this));
    }

    public void addStudent(CourseExecutionStudent courseExecutionStudent) {
        this.students.add(courseExecutionStudent);
        courseExecutionStudent.setCourseExecution(this);
    }

    /*
     * REMOVE_NO_STUDENTS
     */
    public boolean removedNoStudents() {
        if (getState() == AggregateState.DELETED) {
            return getStudents().size() == 0;
        }
        return true;
    }

    public boolean allStudentsAreActive() {
        for (CourseExecutionStudent student : getStudents()) {
            if (!student.isActive()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (!(removedNoStudents() /* && allStudentsAreActive() */)) {
            throw new QuizzesException(QuizzesErrorMessage.INVARIANT_BREAK, getAggregateId());
        }
    }

    @Override
    public void remove() {
        /*
         * CANNOT_REMOVE_IF_STUDENTS
         */
        if (!getStudents().isEmpty()) {
            throw new QuizzesException(QuizzesErrorMessage.CANNOT_DELETE_COURSE_EXECUTION, getAggregateId());
        }
        super.remove();
    }

    public int getCourseQuestionCount() {
        return courseQuestionCount;
    }

    public void setCourseQuestionCount(int courseQuestionCount) {
        this.courseQuestionCount = courseQuestionCount;
    }

    @Override
    public void setVersion(Integer version) {
        // if the course version is null, it means it that we're creating during this
        // transaction
        if (this.courseExecutionCourse != null && this.courseExecutionCourse.getCourseVersion() == null) {
            this.courseExecutionCourse.setCourseVersion(version);
        }
        super.setVersion(version);
    }

    public boolean hasStudent(Integer userAggregateId) {
        for (CourseExecutionStudent student : this.students) {
            if (student.getUserAggregateId().equals(userAggregateId)) {
                return true;
            }
        }
        return false;
    }

    public CourseExecutionStudent findStudent(Integer userAggregateId) {
        for (CourseExecutionStudent student : this.students) {
            if (student.getUserAggregateId().equals(userAggregateId)) {
                return student;
            }
        }
        return null;
    }

    public void removeStudent(Integer userAggregateId) {
        CourseExecutionStudent studentToRemove = null;
        if (!hasStudent(userAggregateId)) {
            throw new QuizzesException(QuizzesErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND, userAggregateId,
                    getAggregateId());
        }
        for (CourseExecutionStudent student : this.students) {
            if (student.getUserAggregateId().equals(userAggregateId)) {
                studentToRemove = student;
            }
        }
        this.students.remove(studentToRemove);
    }
}
