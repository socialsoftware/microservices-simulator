package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class QuizController {
    @Autowired
    private QuizFunctionalities quizFunctionalities;

    @PostMapping("/executions/{executionId}")
    public QuizDto createQuiz(@PathVariable Integer executionId, @RequestBody QuizDto quizDto) throws Exception {
        QuizDto result = quizFunctionalities.createQuiz(executionId, quizDto);
        return result;
    }

    @PostMapping("/quizzes/update")
    public QuizDto updateQuiz(@RequestBody QuizDto quizDto) throws Exception {
        QuizDto result = quizFunctionalities.updateQuiz(quizDto);
        return result;
    }

    @GetMapping("/quizzes/{quizAggregateId}")
    public QuizDto findQuiz(@PathVariable Integer quizAggregateId) {
        QuizDto result = quizFunctionalities.findQuiz(quizAggregateId);
        return result;
    }
}
