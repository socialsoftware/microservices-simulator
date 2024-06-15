package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

public class GetTeachersData extends WorkflowData {
    private List<UserDto> teachers;

    public List<UserDto> getTeachers() {
        return teachers;
    }

    public void setTeachers(List<UserDto> teachers) {
        this.teachers = teachers;
    }
}