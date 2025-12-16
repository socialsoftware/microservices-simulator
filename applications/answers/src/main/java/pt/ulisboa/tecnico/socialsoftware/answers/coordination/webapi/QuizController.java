package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.QuizFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

@RestController
public class QuizController {
    @Autowired
    private QuizFunctionalities quizFunctionalities;

    @PostMapping("/executions/{executionId}")
    public QuizDto createQuiz(@PathVariable Integer executionId, @RequestBody QuizDto quizDto) throws Exception {
        return quizFunctionalities.createQuiz(executionId, quizDto);
    }

    @PostMapping("/quizzes/update")
    public QuizDto updateQuiz(@RequestBody QuizDto quizDto) throws Exception {
        return quizFunctionalities.updateQuiz(quizDto);
    }

    @GetMapping("/quizzes/{quizAggregateId}")
    public QuizDto findQuiz(@PathVariable Integer quizAggregateId) {
        return quizFunctionalities.findQuiz(quizAggregateId);
    }
}
