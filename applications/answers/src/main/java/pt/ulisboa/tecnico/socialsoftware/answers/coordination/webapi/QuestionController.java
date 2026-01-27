package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuestionFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
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
    public List<QuestionDto> searchQuestions(@RequestParam(required = false) String title, @RequestParam(required = false) String content) {
        return questionFunctionalities.searchQuestions(title, content);
    }
}
