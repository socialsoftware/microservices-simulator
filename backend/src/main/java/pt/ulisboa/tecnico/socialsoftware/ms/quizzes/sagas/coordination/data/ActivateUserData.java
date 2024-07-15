package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaUser;

public class ActivateUserData extends WorkflowData {
    private SagaUser user;

    public SagaUser getUser() {
        return user;
    }

    public void setUser(SagaUser user) {
        this.user = user;
    }
}