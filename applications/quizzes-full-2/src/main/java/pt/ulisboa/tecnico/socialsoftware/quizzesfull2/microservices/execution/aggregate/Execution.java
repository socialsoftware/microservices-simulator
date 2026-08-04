package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "executions")
public abstract class Execution extends Aggregate {
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private final Integer courseAggregateId;
    private final String courseName;
    @Enumerated(EnumType.STRING)
    private final CourseType courseType;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExecutionStudent> students = new ArrayList<>();

    public Execution() {
        this.courseAggregateId = null;
        this.courseName = null;
        this.courseType = null;
    }

    public Execution(Integer aggregateId, Integer courseAggregateId, String courseName, CourseType courseType,
                     String acronym, String academicTerm, LocalDateTime endDate) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.courseAggregateId = courseAggregateId;
        this.courseName = courseName;
        this.courseType = courseType;
        this.acronym = acronym;
        this.academicTerm = academicTerm;
        this.endDate = endDate;
    }

    public Execution(Execution other) {
        super(other);
        this.courseAggregateId = other.getCourseAggregateId();
        this.courseName = other.getCourseName();
        this.courseType = other.getCourseType();
        this.acronym = other.getAcronym();
        this.academicTerm = other.getAcademicTerm();
        this.endDate = other.getEndDate();
        this.students = other.getStudents().stream()
                .map(ExecutionStudent::new)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyInvariants() {
        removeNoStudents();
        studentNotAlreadyEnrolled();
    }

    private void removeNoStudents() {
        if (getState() == AggregateState.DELETED && !this.students.isEmpty()) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.REMOVE_NO_STUDENTS);
        }
    }

    private void studentNotAlreadyEnrolled() {
        Set<Integer> distinctStudentIds = this.students.stream()
                .map(ExecutionStudent::getUserAggregateId)
                .collect(Collectors.toSet());
        if (distinctStudentIds.size() != this.students.size()) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.STUDENT_ALREADY_ENROLLED);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
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

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public String getCourseName() {
        return courseName;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public List<ExecutionStudent> getStudents() {
        return students;
    }

    public void setStudents(List<ExecutionStudent> students) {
        this.students = students;
    }

    public void addStudent(ExecutionStudent student) {
        this.students.add(student);
    }
}
