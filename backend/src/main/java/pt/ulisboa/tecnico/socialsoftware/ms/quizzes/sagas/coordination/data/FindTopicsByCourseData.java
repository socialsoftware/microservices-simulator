package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;

public class FindTopicsByCourseData extends WorkflowData {
    private List<TopicDto> topics;

    public List<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicDto> topics) {
        this.topics = topics;
    }
}