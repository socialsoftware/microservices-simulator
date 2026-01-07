package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.AnswerFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@RestController
public class AnswerController {
    @Autowired
    private AnswerFunctionalities answerFunctionalities;

    @PostMapping("/answers/create")
    public AnswerDto createAnswer(@RequestParam Integer executionAggregateId, @RequestParam Integer userAggregateId, @RequestParam Integer quizAggregateId, @RequestBody AnswerDto answerDto) {
        return answerFunctionalities.createAnswer(executionAggregateId, userAggregateId, quizAggregateId, answerDto);
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
    public void deleteAnswer(@PathVariable Integer answerAggregateId) {
        answerFunctionalities.deleteAnswer(answerAggregateId);
    }

    @GetMapping("/answers")
    public List<AnswerDto> searchAnswers(@RequestParam(required = false) Boolean completed, @RequestParam(required = false) Integer executionAggregateId, @RequestParam(required = false) Integer userAggregateId, @RequestParam(required = false) Integer quizAggregateId) {
        return answerFunctionalities.searchAnswers(completed, executionAggregateId, userAggregateId, quizAggregateId);
    }
}
