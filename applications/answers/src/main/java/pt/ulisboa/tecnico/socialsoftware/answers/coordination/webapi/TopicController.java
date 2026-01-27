package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.TopicFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTopicRequestDto;

@RestController
public class TopicController {
    @Autowired
    private TopicFunctionalities topicFunctionalities;

    @PostMapping("/topics/create")
    public TopicDto createTopic(@RequestBody CreateTopicRequestDto createRequest) {
        return topicFunctionalities.createTopic(createRequest);
    }

    @GetMapping("/topics/{topicAggregateId}")
    public TopicDto getTopicById(@PathVariable Integer topicAggregateId) {
        return topicFunctionalities.getTopicById(topicAggregateId);
    }

    @PutMapping("/topics")
    public TopicDto updateTopic(@RequestBody TopicDto topicDto) {
        return topicFunctionalities.updateTopic(topicDto);
    }

    @DeleteMapping("/topics/{topicAggregateId}")
    public void deleteTopic(@PathVariable Integer topicAggregateId) {
        topicFunctionalities.deleteTopic(topicAggregateId);
    }

    @GetMapping("/topics")
    public List<TopicDto> getAllTopics() {
        return topicFunctionalities.getAllTopics();
    }
}
