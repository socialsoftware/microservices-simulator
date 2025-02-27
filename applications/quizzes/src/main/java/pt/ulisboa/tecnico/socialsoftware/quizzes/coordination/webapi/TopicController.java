package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

@RestController
public class TopicController {

    @Autowired
    private TopicFunctionalities topicFunctionalities;

    @GetMapping("/courses/{courseAggregateId}/topics")
    public List<TopicDto> findTopicsByCourseAggregateId(@PathVariable Integer courseAggregateId) {
        return topicFunctionalities.findTopicsByCourseAggregateId(courseAggregateId);
    }

    @PostMapping("/courses/{courseAggregateId}/create")
    public TopicDto createTopic(@PathVariable Integer courseAggregateId, @RequestBody TopicDto topicDto) throws Exception {
        return topicFunctionalities.createTopic(courseAggregateId, topicDto);
    }

    @PostMapping("/topics/update")
    public void updateTopic(@RequestBody TopicDto topicDto) throws Exception {
        topicFunctionalities.updateTopic(topicDto);
    }

    @PostMapping("/topics/{topicAggregateId}/delete")
    public void deleteTopic(@PathVariable Integer topicAggregateId) throws Exception {
        topicFunctionalities.deleteTopic(topicAggregateId);
    }

}
