package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import org.apache.commons.collections4.SetUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate.CausalAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionStudent;

import java.util.HashSet;
import java.util.Set;

@Entity
public class CausalCourseExecution extends CourseExecution implements CausalAggregate {
    public CausalCourseExecution() {
        super();
    }

    public CausalCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        super(aggregateId, courseExecutionDto, courseExecutionCourse);
    }

    public CausalCourseExecution(CausalCourseExecution other) {
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
        CourseExecution committedCourseExecution = (CourseExecution) committedVersion;
        mergeQuizQuestions((CourseExecution) getPrev(), this, committedCourseExecution, this);
        return this;
    }

    private void mergeQuizQuestions(CourseExecution prev, CourseExecution toCommitQuiz, CourseExecution committedQuiz, CourseExecution mergedCourseExecution) {
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
        mergedCourseExecution.setStudents(mergedStudents);
    }
}
