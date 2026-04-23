package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import org.apache.commons.collections4.SetUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;

import java.util.HashSet;
import java.util.Set;

@Entity
public class CausalExecution extends Execution implements CausalAggregate {
    public CausalExecution() {
        super();
    }

    public CausalExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        super(aggregateId, courseExecutionDto, courseExecutionCourse);
    }

    public CausalExecution(CausalExecution other) {
        super(other);
    }

    @Override
    @JsonIgnore
    public Set<String> getMutableFields() {
        return Set.of("students");
    }

    @Override
    @JsonIgnore
    public Set<String[]> getIntentions() {
        return new HashSet<>();
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields) {
        Execution committedExecution = (Execution) committedVersion;
        mergeQuizQuestions((Execution) getPrev(), this, committedExecution, this);
        return this;
    }

    private void mergeQuizQuestions(Execution prev, Execution toCommitQuiz, Execution committedQuiz, Execution mergedExecution) {
        Set<CourseExecutionStudent> prevStudentsPre = new HashSet<>(prev.getStudents());
        Set<CourseExecutionStudent> toCommitStudentsPre = new HashSet<>(toCommitQuiz.getStudents());
        Set<CourseExecutionStudent> committedStudentsPre = new HashSet<>(committedQuiz.getStudents());

        CourseExecutionStudent.syncStudentVersions(prevStudentsPre, toCommitStudentsPre, committedStudentsPre);

        Set<CourseExecutionStudent> prevStudents = new HashSet<>(prevStudentsPre);
        Set<CourseExecutionStudent> toCommitQuizStudents = new HashSet<>(toCommitStudentsPre);
        Set<CourseExecutionStudent> committedQuizStudents = new HashSet<>(committedStudentsPre);


        Set<CourseExecutionStudent> addedStudents =  SetUtils.union(
                SetUtils.difference(toCommitQuizStudents, prevStudents),
                SetUtils.difference(committedQuizStudents, prevStudents)
        );

        Set<CourseExecutionStudent> removedStudents = SetUtils.union(
                SetUtils.difference(prevStudents, toCommitQuizStudents),
                SetUtils.difference(prevStudents, committedQuizStudents)
        );

        Set<CourseExecutionStudent> mergedStudents = SetUtils.union(SetUtils.difference(prevStudents, removedStudents), addedStudents);
        mergedExecution.setStudents(mergedStudents);
    }
}
