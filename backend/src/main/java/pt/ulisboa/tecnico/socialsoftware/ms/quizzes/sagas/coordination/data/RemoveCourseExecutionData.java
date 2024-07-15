package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;

public class RemoveCourseExecutionData extends WorkflowData {
    private SagaCourseExecution courseExecution;

    public SagaCourseExecution getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(SagaCourseExecution courseExecution) {
        this.courseExecution = courseExecution;
    }
}