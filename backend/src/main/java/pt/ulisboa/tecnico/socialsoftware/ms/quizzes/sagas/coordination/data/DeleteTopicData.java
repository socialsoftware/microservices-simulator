package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTopic;

public class DeleteTopicData extends WorkflowData {
    private SagaTopic topic;

    public SagaTopic getTopic() {
        return topic;
    }

    public void setTopic(SagaTopic topic) {
        this.topic = topic;
    }
}