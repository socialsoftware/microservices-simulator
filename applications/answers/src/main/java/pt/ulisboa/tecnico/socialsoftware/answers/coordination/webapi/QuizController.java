package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuizFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateQuizRequestDto;

@RestController
public class QuizController {
    @Autowired
    private QuizFunctionalities quizFunctionalities;

    @PostMapping("/quizs/create")
    public QuizDto createQuiz(@RequestBody CreateQuizRequestDto createRequest) {
        return quizFunctionalities.createQuiz(createRequest);
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
    public List<QuizDto> getAllQuizs() {
        return quizFunctionalities.getAllQuizs();
    }
}
