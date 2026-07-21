package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.subscribe.ExecutionSubscribesAnonymizeStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.subscribe.ExecutionSubscribesDeleteUser;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.subscribe.ExecutionSubscribesUpdateStudentName;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/*
    INTRA-INVARIANTS:
        REMOVE_NO_STUDENTS
        STUDENT_ALREADY_ENROLLED
    INTER-INVARIANTS:
        USER_EXISTS (subscribes to DeleteUserEvent, UpdateStudentNameEvent, AnonymizeStudentEvent)
 */
@Entity
public abstract class Execution extends Aggregate {

    @Column
    private String acronym;

    @Column
    private String academicTerm;

    @Column
    private LocalDateTime endDate;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "execution")
    private ExecutionCourse executionCourse;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExecutionStudent> students = new HashSet<>();

    public Execution() {}

    public Execution(Integer aggregateId, String acronym, String academicTerm, ExecutionCourse executionCourse) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAcronym(acronym);
        setAcademicTerm(academicTerm);
        setExecutionCourse(executionCourse);
    }

    public Execution(Execution other) {
        super(other);
        setAcronym(other.getAcronym());
        setAcademicTerm(other.getAcademicTerm());
        setEndDate(other.getEndDate());
        setExecutionCourse(new ExecutionCourse(other.getExecutionCourse()));
        for (ExecutionStudent student : other.getStudents()) {
            this.students.add(new ExecutionStudent(student));
        }
    }

    private boolean removeNoStudents() {
        if (getState() == AggregateState.DELETED) {
            return students.isEmpty();
        }
        return true;
    }

    private boolean studentAlreadyEnrolled() {
        return students.stream()
                .map(ExecutionStudent::getUserAggregateId)
                .distinct()
                .count() == students.size();
    }

    @Override
    public void verifyInvariants() {
        if (!removeNoStudents()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.REMOVE_NO_STUDENTS);
        }
        if (!studentAlreadyEnrolled()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.STUDENT_ALREADY_ENROLLED);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> subscriptions = new HashSet<>();
        for (ExecutionStudent student : students) {
            subscriptions.add(new ExecutionSubscribesDeleteUser(student));
            subscriptions.add(new ExecutionSubscribesUpdateStudentName(student));
            subscriptions.add(new ExecutionSubscribesAnonymizeStudent(student));
        }
        return subscriptions;
    }

    public String getAcronym() { return acronym; }
    public void setAcronym(String acronym) { this.acronym = acronym; }

    public String getAcademicTerm() { return academicTerm; }
    public void setAcademicTerm(String academicTerm) { this.academicTerm = academicTerm; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public ExecutionCourse getExecutionCourse() { return executionCourse; }
    public void setExecutionCourse(ExecutionCourse executionCourse) {
        this.executionCourse = executionCourse;
        this.executionCourse.setExecution(this);
    }

    public Set<ExecutionStudent> getStudents() { return students; }
    public void setStudents(Set<ExecutionStudent> students) { this.students = students; }

    public void addStudent(ExecutionStudent student) { this.students.add(student); }
    public void removeStudent(ExecutionStudent student) { this.students.remove(student); }
}
