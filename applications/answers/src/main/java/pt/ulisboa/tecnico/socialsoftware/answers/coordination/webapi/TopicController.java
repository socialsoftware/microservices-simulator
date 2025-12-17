package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TopicFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;

@RestController
public class TopicController {
    @Autowired
    private TopicFunctionalities topicFunctionalities;

    @PostMapping("/topics/create")
    public TopicDto createTopic(@RequestParam Integer courseAggregateId, @RequestBody TopicDto topicDto) {
        return topicFunctionalities.createTopic(courseAggregateId, topicDto);
    }

    @GetMapping("/topics/{topicAggregateId}")
    public TopicDto getTopicById(@PathVariable Integer topicAggregateId) {
        return topicFunctionalities.getTopicById(topicAggregateId);
    }

    @PutMapping("/topics/{topicAggregateId}")
    public TopicDto updateTopic(@PathVariable Integer topicAggregateId, @RequestBody TopicDto topicDto) {
        return topicFunctionalities.updateTopic(topicAggregateId, topicDto);
    }

    @DeleteMapping("/topics/{topicAggregateId}")
    public void deleteTopic(@PathVariable Integer topicAggregateId) {
        topicFunctionalities.deleteTopic(topicAggregateId);
    }

    @GetMapping("/topics")
    public List<TopicDto> searchTopics(@RequestParam(required = false) String name, @RequestParam(required = false) Integer courseAggregateId) {
        return topicFunctionalities.searchTopics(name, courseAggregateId);
    }
}
