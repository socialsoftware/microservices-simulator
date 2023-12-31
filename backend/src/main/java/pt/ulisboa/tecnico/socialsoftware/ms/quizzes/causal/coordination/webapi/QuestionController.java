package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.QuestionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;

import java.util.List;

@RestController
public class QuestionController {
    @Autowired
    private QuestionFunctionalities questionFunctionalities;

    @GetMapping("/questions/{aggregateId}")
    public QuestionDto findQuestionByAggregateId(@PathVariable Integer aggregateId) {
        return questionFunctionalities.findQuestionByAggregateId(aggregateId);
    }

    @GetMapping("/courses/{courseAggregateId}/questions")
    public List<QuestionDto> findQuestionsByCourseAggregateId(@PathVariable Integer courseAggregateId) {
        return questionFunctionalities.findQuestionsByCourseAggregateId(courseAggregateId);
    }

    @PostMapping("/courses/{courseAggregateId}/questions/create")
    public QuestionDto createQuestion(@PathVariable Integer courseAggregateId, @RequestBody QuestionDto questionDto) {
        return questionFunctionalities.createQuestion(courseAggregateId, questionDto);
    }

    @PostMapping("/questions/update")
    public void updateQuestion(@RequestBody QuestionDto questionDto) {
        questionFunctionalities.updateQuestion(questionDto);
    }

    @PostMapping("/questions/{questionAggregateId}")
    public void removeQuestion(@PathVariable Integer questionAggregateId) {
        questionFunctionalities.removeQuestion(questionAggregateId);
    }

    @PostMapping("/questions/{questionAggregateId}/updateTopics")
    public void updateQuestionTopics(@PathVariable Integer courseAggregateId, @RequestParam List<Integer> topicIds) {
        questionFunctionalities.updateQuestionTopics(courseAggregateId, topicIds);
    }
}
