package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaUser;

public class UpdateStudentNameData extends WorkflowData {
    private SagaUser student;
    private CourseExecution oldCourseExecution;

    public SagaUser getStudent() {
        return student;
    }

    public void setStudent(SagaUser student) {
        this.student = student;
    }

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}

