package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateAnswerRequestDto;

@RestController
public class AnswerController {
    @Autowired
    private AnswerFunctionalities answerFunctionalities;

    @PostMapping("/answers/create")
    @ResponseStatus(HttpStatus.CREATED)
    public AnswerDto createAnswer(@RequestBody CreateAnswerRequestDto createRequest) {
        return answerFunctionalities.createAnswer(createRequest);
    }

    @GetMapping("/answers/{answerAggregateId}")
    public AnswerDto getAnswerById(@PathVariable Integer answerAggregateId) {
        return answerFunctionalities.getAnswerById(answerAggregateId);
    }

    @PutMapping("/answers")
    public AnswerDto updateAnswer(@RequestBody AnswerDto answerDto) {
        return answerFunctionalities.updateAnswer(answerDto);
    }

    @DeleteMapping("/answers/{answerAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswer(@PathVariable Integer answerAggregateId) {
        answerFunctionalities.deleteAnswer(answerAggregateId);
    }

    @GetMapping("/answers")
    public List<AnswerDto> getAllAnswers() {
        return answerFunctionalities.getAllAnswers();
    }

    @PostMapping("/answers/{answerId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public AnswerQuestionDto addAnswerQuestion(@PathVariable Integer answerId, @RequestParam Integer questionAggregateId, @RequestBody AnswerQuestionDto questionDto) {
        return answerFunctionalities.addAnswerQuestion(answerId, questionAggregateId, questionDto);
    }

    @PostMapping("/answers/{answerId}/questions/batch")
    public List<AnswerQuestionDto> addAnswerQuestions(@PathVariable Integer answerId, @RequestBody List<AnswerQuestionDto> questionDtos) {
        return answerFunctionalities.addAnswerQuestions(answerId, questionDtos);
    }

    @GetMapping("/answers/{answerId}/questions/{questionAggregateId}")
    public AnswerQuestionDto getAnswerQuestion(@PathVariable Integer answerId, @PathVariable Integer questionAggregateId) {
        return answerFunctionalities.getAnswerQuestion(answerId, questionAggregateId);
    }

    @PutMapping("/answers/{answerId}/questions/{questionAggregateId}")
    public AnswerQuestionDto updateAnswerQuestion(@PathVariable Integer answerId, @PathVariable Integer questionAggregateId, @RequestBody AnswerQuestionDto questionDto) {
        return answerFunctionalities.updateAnswerQuestion(answerId, questionAggregateId, questionDto);
    }

    @DeleteMapping("/answers/{answerId}/questions/{questionAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAnswerQuestion(@PathVariable Integer answerId, @PathVariable Integer questionAggregateId) {
        answerFunctionalities.removeAnswerQuestion(answerId, questionAggregateId);
    }
}
