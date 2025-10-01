package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class QuestionController {
    @Autowired
    private QuestionFunctionalities questionFunctionalities;

    @GetMapping("/questions/{aggregateId}")
    public QuestionDto findQuestionByAggregateId(@PathVariable Integer aggregateId) {
        QuestionDto result = questionFunctionalities.findQuestionByAggregateId(aggregateId);
        return result;
    }

    @GetMapping("/courses/{courseAggregateId}/questions")
    public List<QuestionDto> findQuestionsByCourseAggregateId(@PathVariable Integer courseAggregateId) {
        List<QuestionDto> result = questionFunctionalities.findQuestionsByCourseAggregateId(courseAggregateId);
        return result;
    }

    @PostMapping("/courses/{courseAggregateId}/questions/create")
    public QuestionDto createQuestion(@PathVariable Integer courseAggregateId, @RequestBody QuestionDto questionDto) throws Exception {
        QuestionDto result = questionFunctionalities.createQuestion(courseAggregateId, questionDto);
        return result;
    }

    @PostMapping("/questions/update")
    public void updateQuestion(@RequestBody QuestionDto questionDto) throws Exception {
        questionFunctionalities.updateQuestion(questionDto);
    }

    @PostMapping("/questions/{questionAggregateId}")
    public void removeQuestion(@PathVariable Integer questionAggregateId) throws Exception {
        questionFunctionalities.removeQuestion(questionAggregateId);
    }

    @PostMapping("/questions/{questionAggregateId}/updateTopics")
    public void updateQuestionTopics(@PathVariable Integer courseAggregateId, @RequestParam String topicIds) throws Exception {
        questionFunctionalities.updateQuestionTopics(courseAggregateId, topicIds);
    }
}
