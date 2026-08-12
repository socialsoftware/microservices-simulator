package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.functionalities.QuizFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto;

@RestController
public class QuizController {
    @Autowired
    private QuizFunctionalities quizFunctionalities;

    @PostMapping("/quizs/create")
    @ResponseStatus(HttpStatus.CREATED)
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuiz(@PathVariable Integer quizAggregateId) {
        quizFunctionalities.deleteQuiz(quizAggregateId);
    }

    @GetMapping("/quizs")
    public List<QuizDto> getAllQuizs() {
        return quizFunctionalities.getAllQuizs();
    }

    @PostMapping("/quizs/{quizId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuizQuestionDto addQuizQuestion(@PathVariable Integer quizId, @RequestParam Integer questionAggregateId, @RequestBody QuizQuestionDto questionDto) {
        return quizFunctionalities.addQuizQuestion(quizId, questionAggregateId, questionDto);
    }

    @PostMapping("/quizs/{quizId}/questions/batch")
    public List<QuizQuestionDto> addQuizQuestions(@PathVariable Integer quizId, @RequestBody List<QuizQuestionDto> questionDtos) {
        return quizFunctionalities.addQuizQuestions(quizId, questionDtos);
    }

    @GetMapping("/quizs/{quizId}/questions/{questionAggregateId}")
    public QuizQuestionDto getQuizQuestion(@PathVariable Integer quizId, @PathVariable Integer questionAggregateId) {
        return quizFunctionalities.getQuizQuestion(quizId, questionAggregateId);
    }

    @PutMapping("/quizs/{quizId}/questions/{questionAggregateId}")
    public QuizQuestionDto updateQuizQuestion(@PathVariable Integer quizId, @PathVariable Integer questionAggregateId, @RequestBody QuizQuestionDto questionDto) {
        return quizFunctionalities.updateQuizQuestion(quizId, questionAggregateId, questionDto);
    }

    @DeleteMapping("/quizs/{quizId}/questions/{questionAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQuizQuestion(@PathVariable Integer quizId, @PathVariable Integer questionAggregateId) {
        quizFunctionalities.removeQuizQuestion(quizId, questionAggregateId);
    }
}
