package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.webapi;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionFunctionalitiesInterface;

@RestController
public class QuestionController {
    @Autowired
    private QuestionFunctionalitiesInterface questionFunctionalities;

    @GetMapping("/questions/{aggregateId}")
    public QuestionDto findQuestionByAggregateId(@PathVariable Integer aggregateId) {
        return questionFunctionalities.findQuestionByAggregateId(aggregateId);
    }

    @GetMapping("/courses/{courseAggregateId}/questions")
    public List<QuestionDto> findQuestionsByCourseAggregateId(@PathVariable Integer courseAggregateId) {
        return questionFunctionalities.findQuestionsByCourseAggregateId(courseAggregateId);
    }

    @PostMapping("/courses/{courseAggregateId}/questions/create")
    public QuestionDto createQuestion(@PathVariable Integer courseAggregateId, @RequestBody QuestionDto questionDto) throws Exception {
        return questionFunctionalities.createQuestion(courseAggregateId, questionDto);
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
    public void updateQuestionTopics(@PathVariable Integer courseAggregateId, @RequestParam List<Integer> topicIds) throws Exception {
        questionFunctionalities.updateQuestionTopics(courseAggregateId, topicIds);
    }
}
