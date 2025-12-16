package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TopicFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

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
