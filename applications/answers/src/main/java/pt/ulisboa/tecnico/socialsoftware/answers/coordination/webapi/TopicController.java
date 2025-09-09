package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
@RequestMapping("/api/topic")
public class TopicController {

@Autowired
private TopicService topicService;

@GetMapping(value = "/topics")
public ResponseEntity<List<TopicDto>> getAllTopics(
        ) {
        List<TopicDto> result = topicService.getAllTopics();
        return ResponseEntity.ok(result);
        }

@GetMapping(value = "/topics/{id}")
public ResponseEntity<TopicDto> getTopic(
        @PathVariable Long id
        ) throws Exception {
        TopicDto result = topicService.getTopic(id);
        return ResponseEntity.ok(result);
        }

@PostMapping(value = "/topics")
public ResponseEntity<TopicDto> createTopic(
        @RequestBody TopicDto topicDto
        ) throws Exception {
        TopicDto result = topicService.createTopic(topicDto);
        return ResponseEntity.ok(result);
        }

@PutMapping(value = "/topics/{id}")
public ResponseEntity<TopicDto> updateTopic(
        @PathVariable Long id,
        @RequestBody TopicDto topicDto
        ) throws Exception {
        TopicDto result = topicService.updateTopic(id,
        topicDto);
        return ResponseEntity.ok(result);
        }

@DeleteMapping(value = "/topics/{id}")
public ResponseEntity<Void> deleteTopic(
        @PathVariable Long id
        ) throws Exception {
        topicService.deleteTopic(id);
        return ResponseEntity.ok().build();
        }

        }