package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.util.List;

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
