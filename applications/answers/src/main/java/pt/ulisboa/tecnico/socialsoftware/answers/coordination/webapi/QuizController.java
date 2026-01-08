package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuizFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

@RestController
public class QuizController {
    @Autowired
    private QuizFunctionalities quizFunctionalities;

    @PostMapping("/quizs/create")
    public QuizDto createQuiz(@RequestBody QuizDto quizDto) {
        return quizFunctionalities.createQuiz(quizDto);
    }

    @GetMapping("/quizs/{quizAggregateId}")
    public QuizDto getQuizById(@PathVariable Integer quizAggregateId) {
        return quizFunctionalities.getQuizById(quizAggregateId);
    }

    @PutMapping("/quizs")
    public QuizDto updateQuiz(@RequestBody QuizDto quizDto) {
        return quizFunctionalities.updateQuiz(quizDto);
    }

    @DeleteMapping("/quizs/{quizAggregateId}")
    public void deleteQuiz(@PathVariable Integer quizAggregateId) {
        quizFunctionalities.deleteQuiz(quizAggregateId);
    }

    @GetMapping("/quizs")
    public List<QuizDto> searchQuizs(@RequestParam(required = false) String title, @RequestParam(required = false) QuizType quizType, @RequestParam(required = false) Integer executionAggregateId) {
        return quizFunctionalities.searchQuizs(title, quizType, executionAggregateId);
    }
}
