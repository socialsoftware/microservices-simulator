package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.functionalities.TopicFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto;

@RestController
public class TopicController {
    @Autowired
    private TopicFunctionalities topicFunctionalities;

    @PostMapping("/topics/create")
    @ResponseStatus(HttpStatus.CREATED)
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable Integer topicAggregateId) {
        topicFunctionalities.deleteTopic(topicAggregateId);
    }

    @GetMapping("/topics")
    public List<TopicDto> getAllTopics() {
        return topicFunctionalities.getAllTopics();
    }
}
