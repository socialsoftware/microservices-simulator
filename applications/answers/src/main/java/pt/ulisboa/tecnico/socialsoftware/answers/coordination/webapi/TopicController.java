package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TopicFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class TopicController {
    @Autowired
    private TopicFunctionalities topicFunctionalities;

    @GetMapping("/courses/{courseAggregateId}/topics")
    public List<TopicDto> findTopicsByCourseAggregateId(@PathVariable Integer courseAggregateId) {
        List<TopicDto> result = topicFunctionalities.findTopicsByCourseAggregateId(courseAggregateId);
        return result;
    }

    @PostMapping("/courses/{courseAggregateId}/create")
    public TopicDto createTopic(@PathVariable Integer courseAggregateId, @RequestBody TopicDto topicDto) throws Exception {
        TopicDto result = topicFunctionalities.createTopic(courseAggregateId, topicDto);
        return result;
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
