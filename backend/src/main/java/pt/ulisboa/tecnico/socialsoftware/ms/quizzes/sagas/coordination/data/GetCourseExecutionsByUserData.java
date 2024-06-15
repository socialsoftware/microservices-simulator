package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;

public class GetCourseExecutionsByUserData extends WorkflowData {
    private Set<CourseExecutionDto> courseExecutions;

    public Set<CourseExecutionDto> getCourseExecutions() {
        return courseExecutions;
    }

    public void setCourseExecutions(Set<CourseExecutionDto> courseExecutions) {
        this.courseExecutions = courseExecutions;
    }
}