package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuestionFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuestionRequestDto;

@RestController
public class QuestionController {
    @Autowired
    private QuestionFunctionalities questionFunctionalities;

    @PostMapping("/questions/create")
    public QuestionDto createQuestion(@RequestBody CreateQuestionRequestDto createRequest) {
        return questionFunctionalities.createQuestion(createRequest);
    }

    @GetMapping("/questions/{questionAggregateId}")
    public QuestionDto getQuestionById(@PathVariable Integer questionAggregateId) {
        return questionFunctionalities.getQuestionById(questionAggregateId);
    }

    @PutMapping("/questions")
    public QuestionDto updateQuestion(@RequestBody QuestionDto questionDto) {
        return questionFunctionalities.updateQuestion(questionDto);
    }

    @DeleteMapping("/questions/{questionAggregateId}")
    public void deleteQuestion(@PathVariable Integer questionAggregateId) {
        questionFunctionalities.deleteQuestion(questionAggregateId);
    }

    @GetMapping("/questions")
    public List<QuestionDto> getAllQuestions() {
        return questionFunctionalities.getAllQuestions();
    }

    @PostMapping("/questions/{questionId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionTopicDto addQuestionTopic(@PathVariable Integer questionId, @RequestParam Integer topicAggregateId, @RequestBody QuestionTopicDto topicDto) {
        return questionFunctionalities.addQuestionTopic(questionId, topicAggregateId, topicDto);
    }

    @PostMapping("/questions/{questionId}/topics/batch")
    public List<QuestionTopicDto> addQuestionTopics(@PathVariable Integer questionId, @RequestBody List<QuestionTopicDto> topicDtos) {
        return questionFunctionalities.addQuestionTopics(questionId, topicDtos);
    }

    @GetMapping("/questions/{questionId}/topics/{topicAggregateId}")
    public QuestionTopicDto getQuestionTopic(@PathVariable Integer questionId, @PathVariable Integer topicAggregateId) {
        return questionFunctionalities.getQuestionTopic(questionId, topicAggregateId);
    }

    @PutMapping("/questions/{questionId}/topics/{topicAggregateId}")
    public QuestionTopicDto updateQuestionTopic(@PathVariable Integer questionId, @PathVariable Integer topicAggregateId, @RequestBody QuestionTopicDto topicDto) {
        return questionFunctionalities.updateQuestionTopic(questionId, topicAggregateId, topicDto);
    }

    @DeleteMapping("/questions/{questionId}/topics/{topicAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuestionTopic(@PathVariable Integer questionId, @PathVariable Integer topicAggregateId) {
        questionFunctionalities.removeQuestionTopic(questionId, topicAggregateId);
    }

    @PostMapping("/questions/{questionId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    public OptionDto addQuestionOption(@PathVariable Integer questionId, @RequestParam Integer key, @RequestBody OptionDto optionDto) {
        return questionFunctionalities.addQuestionOption(questionId, key, optionDto);
    }

    @PostMapping("/questions/{questionId}/options/batch")
    public List<OptionDto> addQuestionOptions(@PathVariable Integer questionId, @RequestBody List<OptionDto> optionDtos) {
        return questionFunctionalities.addQuestionOptions(questionId, optionDtos);
    }

    @GetMapping("/questions/{questionId}/options/{key}")
    public OptionDto getQuestionOption(@PathVariable Integer questionId, @PathVariable Integer key) {
        return questionFunctionalities.getQuestionOption(questionId, key);
    }

    @PutMapping("/questions/{questionId}/options/{key}")
    public OptionDto updateQuestionOption(@PathVariable Integer questionId, @PathVariable Integer key, @RequestBody OptionDto optionDto) {
        return questionFunctionalities.updateQuestionOption(questionId, key, optionDto);
    }

    @DeleteMapping("/questions/{questionId}/options/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuestionOption(@PathVariable Integer questionId, @PathVariable Integer key) {
        questionFunctionalities.removeQuestionOption(questionId, key);
    }
}
